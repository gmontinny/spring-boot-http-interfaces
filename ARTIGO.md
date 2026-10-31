# Clientes HTTP Declarativos no Ecossistema Spring: OpenFeign vs Http Interface

**Autor:** Gustavo Montinny  
**Stack:** Java 25 · Spring Boot 4.1.2 · Spring Cloud 2025.1.4  
**Repositório:** [github.com/gmontinny/http-interfaces](https://github.com/gmontinny/http-interfaces)

---

## Introdução

A integração entre microsserviços e APIs externas é uma das tarefas mais recorrentes no desenvolvimento backend moderno. Durante anos, o **OpenFeign** — incorporado ao ecossistema Spring Cloud — foi a escolha dominante para criar clientes HTTP declarativos em aplicações Java. Com o lançamento do **Spring Framework 6** e do **Spring Boot 3**, surgiu uma alternativa nativa: o **Http Interface**, que elimina a dependência do Spring Cloud e oferece suporte tanto ao modelo síncrono quanto ao reativo dentro do próprio framework.

Este artigo explora as duas abordagens de forma prática, com código real extraído de um projeto que integra ambas simultaneamente contra a API pública [ViaCep](https://viacep.com.br). O objetivo não é eleger um vencedor absoluto, mas evidenciar as diferenças arquiteturais, os trade-offs de cada abordagem e os cenários em que cada uma se destaca.

---

## Contexto do Projeto

O projeto `http-interfaces` foi construído com o propósito explícito de comparar as duas abordagens lado a lado. Ambos os clientes consomem o mesmo endpoint da API ViaCep, persistem o resultado no PostgreSQL e registram no campo `fetchedVia` qual cliente foi utilizado — tornando a diferença visível nos dados.

**Stack completa:**

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 | Linguagem |
| Spring Boot | 4.1.2 | Framework principal |
| Spring Cloud | 2025.1.4 | OpenFeign |
| PostgreSQL | 16 | Banco de dados |
| Liquibase | (gerenciado pelo Boot) | Migrações de schema |
| JWT (jjwt) | 0.12.6 | Autenticação stateless |
| Bucket4j | 8.10.1 | Rate limiting por IP |
| Springdoc OpenAPI | 3.1.0 | Swagger UI |
| OpenCSV | 5.9 | Importação de CSVs |
| HashiCorp Vault | 1.17 | Gerenciamento de secrets |

A arquitetura segue o modelo em camadas: `presentation` → `domain` → `infrastructure`, com os clientes HTTP isolados no pacote `infrastructure`.

```
src/main/java/br/com/httpinterfaces/
├── infrastructure/
│   ├── feign/           # ViaCepFeignClient
│   └── httpinterface/   # ViaCepHttpInterface
```

---

## OpenFeign — O Veterano do Spring Cloud

### Origem e Funcionamento

O OpenFeign nasceu como projeto independente da Netflix e foi posteriormente integrado ao Spring Cloud. Ele permite definir um cliente HTTP como uma interface Java anotada — o framework gera o proxy em tempo de execução, eliminando a necessidade de escrever código de chamada HTTP manualmente.

No Spring Cloud, a ativação é feita com `@EnableFeignClients` na classe principal da aplicação, e cada cliente é declarado com `@FeignClient`.

### Dependência

```groovy
// build.gradle
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:2025.1.4"
    }
}
```

A dependência traz consigo todo o ecossistema Spring Cloud — incluindo balanceamento de carga, service discovery e integração com Resilience4j para circuit breaker e retry.

### Implementação no Projeto

```java
@FeignClient(name = "viacep-feign", url = "${viacep.base-url}")
public interface ViaCepFeignClient {

    @GetMapping("/ws/{cep}/json/")
    ViaCepResponse findByCep(@PathVariable("cep") String cep);
}
```

Três pontos merecem atenção:

1. **`@FeignClient`** declara o cliente com um nome lógico e a URL base lida do `application.yml`
2. **`@GetMapping`** é a mesma anotação usada nos controllers Spring MVC — não há nova sintaxe para aprender
3. O proxy é criado **automaticamente** pelo Spring Cloud no startup da aplicação

A configuração no `application.yml` é mínima:

```yaml
viacep:
  base-url: https://viacep.com.br
```

### Como o Proxy é Gerado

O Spring Cloud escaneia todas as interfaces anotadas com `@FeignClient` durante o bootstrap da aplicação e registra um bean para cada uma. Internamente, o Feign usa um `InvocationHandler` do Java para interceptar as chamadas de método e traduzi-las em requisições HTTP, respeitando as anotações Spring MVC presentes na interface.

### Pontos Positivos

- **Maturidade**: amplamente adotado em produção há mais de uma década
- **Zero configuração manual**: o proxy é criado e registrado automaticamente
- **Anotações familiares**: usa `@GetMapping`, `@PostMapping`, `@PathVariable` — o mesmo vocabulário dos controllers
- **Integração nativa com Resilience4j**: circuit breaker, retry e fallback configuráveis via propriedades
- **Suporte a interceptors**: `RequestInterceptor` para adicionar headers globais (ex.: autenticação)
- **Logging detalhado**: nível de log configurável por cliente (`NONE`, `BASIC`, `HEADERS`, `FULL`)
- **Ecossistema rico**: integração com Eureka, Consul, Ribbon e demais componentes Spring Cloud

### Pontos Negativos

- **Dependência pesada**: requer `spring-cloud-starter-openfeign` e todo o BOM do Spring Cloud
- **Overhead de startup**: o scan de `@FeignClient` e a criação dos proxies aumentam o tempo de inicialização
- **Acoplamento ao Spring Cloud**: em projetos que não usam Spring Cloud, a dependência é desnecessária
- **Modelo apenas síncrono por padrão**: suporte reativo existe, mas requer configuração adicional com `feign-reactor`
- **Versioning complexo**: o BOM do Spring Cloud tem ciclo de vida independente do Spring Boot, exigindo atenção ao alinhamento de versões

---

## Http Interface — O Nativo do Spring 6

### Origem e Funcionamento

Introduzido no **Spring Framework 6.0** (novembro de 2022) e disponível no **Spring Boot 3.0+**, o Http Interface é a resposta do próprio Spring à necessidade de um cliente HTTP declarativo sem dependências externas. Ele usa a mesma filosofia do Feign — interfaces anotadas com geração de proxy em runtime — mas com anotações próprias e integração direta com `RestClient` (síncrono) ou `WebClient` (reativo).

A diferença fundamental: **o proxy não é criado automaticamente**. O desenvolvedor configura explicitamente o `HttpServiceProxyFactory`, escolhendo o transporte HTTP subjacente.

### Dependência

```groovy
// Nenhuma dependência adicional necessária
// Já incluso em:
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
// ou
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

Esta é uma das vantagens mais significativas: zero overhead de dependência.

### Implementação no Projeto

**A interface:**

```java
@HttpExchange(url = "/ws")
public interface ViaCepHttpInterface {

    @GetExchange("/{cep}/json/")
    ViaCepResponse findByCep(String cep);
}
```

**A configuração do proxy:**

```java
@Configuration
public class HttpInterfaceConfig {

    @Value("${viacep.base-url}")
    private String viaCepBaseUrl;

    @Bean
    public ViaCepHttpInterface viaCepHttpInterface() {
        RestClient restClient = RestClient.builder()
                .baseUrl(viaCepBaseUrl)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(ViaCepHttpInterface.class);
    }
}
```

Três pontos merecem atenção:

1. **`@HttpExchange`** substitui `@FeignClient` — define a URL base do contrato
2. **`@GetExchange`** substitui `@GetMapping` — há equivalentes para todos os métodos HTTP
3. O proxy é criado **manualmente** via `HttpServiceProxyFactory` — o desenvolvedor tem controle total sobre o transporte

### Tabela de Equivalência de Anotações

| Spring MVC / Feign | Http Interface |
|---|---|
| `@GetMapping` | `@GetExchange` |
| `@PostMapping` | `@PostExchange` |
| `@PutMapping` | `@PutExchange` |
| `@DeleteMapping` | `@DeleteExchange` |
| `@PatchMapping` | `@PatchExchange` |
| `@RequestMapping` | `@HttpExchange` |

### Suporte Reativo

Uma das capacidades mais relevantes do Http Interface é o suporte nativo ao modelo reativo com `WebClient`:

```java
// Versão reativa — mesmo contrato, transporte diferente
@Bean
public ViaCepHttpInterface viaCepHttpInterfaceReactive() {
    WebClient webClient = WebClient.builder()
            .baseUrl(viaCepBaseUrl)
            .build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();

    return factory.createClient(ViaCepHttpInterface.class);
}
```

A **interface não muda** — apenas o adaptador de transporte. Isso permite migrar de síncrono para reativo sem alterar o contrato da API.

### Pontos Positivos

- **Zero dependência extra**: incluso no Spring Framework 6+ sem nenhum artefato adicional
- **Controle explícito**: o desenvolvedor configura timeouts, interceptors e serialização diretamente no `RestClient` ou `WebClient`
- **Suporte dual**: síncrono (`RestClient`) e reativo (`WebClient`) com o mesmo contrato de interface
- **Menor footprint**: sem o overhead do Spring Cloud no classpath
- **Startup mais rápido**: sem scan automático de clientes
- **Alinhamento com o Spring moderno**: `RestClient` foi introduzido no Spring 6.1 como substituto do `RestTemplate`
- **Testabilidade**: o bean é um `@Bean` comum — fácil de mockar com `@MockitoBean`

### Pontos Negativos

- **Configuração manual obrigatória**: cada cliente requer um `@Bean` explícito com `HttpServiceProxyFactory`
- **Sem circuit breaker nativo**: Resilience4j precisa ser configurado manualmente via decorators
- **Sem retry automático**: requer implementação manual ou integração com Spring Retry
- **Ecossistema menor**: sem a maturidade e os recursos prontos do Spring Cloud
- **Curva de aprendizado**: a configuração do `HttpServiceProxyFactory` é menos intuitiva que `@FeignClient`

---

## Comparação Direta — Lado a Lado

### Código

| Aspecto | OpenFeign | Http Interface |
|---|---|---|
| Declaração do cliente | `@FeignClient(name=..., url=...)` | `@HttpExchange(url=...)` |
| Método GET | `@GetMapping("/path")` | `@GetExchange("/path")` |
| Criação do proxy | Automática via `@EnableFeignClients` | Manual via `HttpServiceProxyFactory` |
| Configuração de URL | Atributo `url` no `@FeignClient` | `RestClient.builder().baseUrl(...)` |
| Parâmetros de path | Inferidos pelo nome (sem anotação) | `@PathVariable` obrigatório |

### Arquitetura

| Aspecto | OpenFeign | Http Interface |
|---|---|---|
| Origem | Spring Cloud (Netflix OSS) | Spring Framework 6+ (nativo) |
| Dependência | `spring-cloud-starter-openfeign` | Nenhuma |
| Transporte | Feign HTTP client | `RestClient` ou `WebClient` |
| Modelo de execução | Síncrono | Síncrono ou Reativo |
| Circuit breaker | Nativo via Resilience4j | Manual |
| Retry | Nativo via Resilience4j | Manual |
| Service discovery | Integrado (Eureka, Consul) | Não incluso |

### Operacional

| Aspecto | OpenFeign | Http Interface |
|---|---|---|
| Tempo de startup | Maior (scan de clientes) | Menor |
| Footprint de memória | Maior (Spring Cloud no classpath) | Menor |
| Logging de requisições | Configurável por cliente | Via `RestClient` interceptors |
| Compatibilidade Spring Boot 4 | Sim (Spring Cloud 2025.x) | Sim (nativo) |

---

## O Campo `fetchedVia` — Diferença Visível nos Dados

Uma das decisões de design mais interessantes do projeto é o campo `fetchedVia` na entidade `Address`. Ele registra qual cliente foi usado para buscar o CEP, tornando a diferença entre as abordagens observável diretamente no banco de dados.

```java
@Entity
@Table(name = "addresses")
public class Address {
    // ... outros campos ...

    /** "FEIGN" ou "HTTP_INTERFACE" */
    @Column(name = "fetched_via", nullable = false, length = 20)
    private String fetchedVia;
}
```

O `AddressService` expõe dois métodos distintos:

```java
@Transactional
public AddressResponse findByCepViaFeign(String cep) {
    ViaCepResponse response = feignClient.findByCep(sanitizeCep(cep));
    validateResponse(response, cep);
    Address saved = addressRepository.save(addressMapper.toEntity(response, "FEIGN"));
    return addressMapper.toResponse(saved);
}

@Transactional
public AddressResponse findByCepViaHttpInterface(String cep) {
    ViaCepResponse response = httpInterface.findByCep(sanitizeCep(cep));
    validateResponse(response, cep);
    Address saved = addressRepository.save(addressMapper.toEntity(response, "HTTP_INTERFACE"));
    return addressMapper.toResponse(saved);
}
```

Do ponto de vista do serviço, as duas chamadas são **idênticas** — a diferença está apenas no bean injetado. Isso demonstra que ambas as abordagens produzem o mesmo contrato de uso para o código consumidor.

Os endpoints expostos permitem comparar os resultados em tempo real:

```
GET /api/v1/addresses/feign/01310100
GET /api/v1/addresses/http-interface/01310100
GET /api/v1/addresses/source/FEIGN
GET /api/v1/addresses/source/HTTP_INTERFACE
```

---

## Segurança — JWT Stateless com Spring Security 7

O projeto implementa autenticação stateless com JWT, com uma particularidade relevante para quem migra do Spring Boot 3: o construtor de `DaoAuthenticationProvider` foi alterado no Spring Security 7.

```java
// Spring Boot 3 / Spring Security 6
var provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService);

// Spring Boot 4 / Spring Security 7
var provider = new DaoAuthenticationProvider(userDetailsService);
```

O `UserDetailsService` agora é obrigatório no construtor — o construtor sem argumentos foi removido. Esta é uma das breaking changes mais comuns na migração para Spring Boot 4.

O fluxo de autenticação:

1. `POST /api/v1/auth/login` → valida credenciais → retorna `accessToken` (24h) + `refreshToken` (7d)
2. Cada requisição passa pelo `JwtAuthFilter`, que extrai o username do token e carrega as authorities do banco via `UserDetailsService`
3. O refresh token tem **rotação**: cada uso gera um novo token e invalida o anterior

---

## Gerenciamento de Schema com Liquibase

Todas as alterações de banco são versionadas via Liquibase, incluindo dados de segurança. Esta é uma prática fundamental que o projeto demonstra na prática.

```
db/changelog/
├── 001-create-users-roles.xml      # Tabelas + usuário admin padrão
├── 002-create-products.xml         # Tabela products
├── 003-create-addresses.xml        # Tabela addresses
├── 004-alter-products-rating.xml   # NUMERIC(3,1) → NUMERIC(4,2)
└── 005-grant-admin-to-gmontinny.xml # Promoção de usuário para ROLE_ADMIN
```

A migration `005` ilustra um padrão importante: usar `preConditions onFail="MARK_RAN"` para tornar o changeSet idempotente e seguro para ambientes onde o usuário pode não existir:

```xml
<changeSet id="005-grant-admin-to-gmontinny" author="gmontinny">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="1">
            SELECT COUNT(*) FROM users WHERE username = 'gmontinny'
        </sqlCheck>
    </preConditions>
    <sql>
        INSERT INTO user_roles (user_id, role_id)
        SELECT u.id, r.id FROM users u, roles r
        WHERE u.username = 'gmontinny' AND r.name = 'ROLE_ADMIN'
        ON CONFLICT DO NOTHING
    </sql>
</changeSet>
```

**Regra de ouro:** alterações de dados de segurança (roles, permissões) nunca devem ser feitas via SQL direto no banco. Liquibase garante rastreabilidade, reprodutibilidade e histórico auditável.

---

## Rate Limiting com Bucket4j

O projeto implementa rate limiting por IP usando o algoritmo **Token Bucket** via Bucket4j, sem dependência de Redis ou qualquer armazenamento externo:

- **Limite**: 20 requisições por minuto por IP
- **Rotas ignoradas**: `/swagger-ui/**` e `/api-docs/**`
- **Resposta ao exceder**: `HTTP 429 Too Many Requests`

O algoritmo Token Bucket é adequado para APIs públicas porque permite rajadas curtas (burst) enquanto mantém uma taxa média controlada — diferente do Fixed Window, que pode permitir o dobro do limite em torno da virada da janela.

---

## Springdoc OpenAPI 3.1.0 — Compatibilidade com Spring Boot 4

Uma das armadilhas mais comuns na migração para Spring Boot 4 é a incompatibilidade do Springdoc:

| Springdoc | Spring Boot compatível |
|---|---|
| `2.x` | 2.x / 3.x |
| `3.1.0+` | 4.x |

O Springdoc 2.x referencia `WebMvcProperties` que foi movida no Spring Framework 7, causando `ClassNotFoundException` no startup. A solução é usar `springdoc-openapi-starter-webmvc-ui:3.1.0+`.

---

## Quando Usar Cada Abordagem

### Use OpenFeign quando:

- O projeto já usa Spring Cloud (Eureka, Config Server, Gateway)
- Você precisa de circuit breaker e retry com configuração mínima via Resilience4j
- A equipe já conhece o ecossistema Spring Cloud
- O projeto é um microsserviço que se beneficia de service discovery
- Você precisa de logging detalhado de requisições HTTP com configuração simples

### Use Http Interface quando:

- O projeto é Spring Boot 3+ ou 4+ sem Spring Cloud
- Você quer minimizar dependências e footprint da aplicação
- Precisa de suporte reativo nativo com `WebClient`
- Quer controle explícito sobre timeouts e configuração do cliente HTTP
- O projeto é uma aplicação standalone ou monolito modular
- Está construindo uma biblioteca ou SDK que não deve impor Spring Cloud como dependência transitiva

---

## Conclusão

O OpenFeign e o Http Interface resolvem o mesmo problema — clientes HTTP declarativos — mas com filosofias distintas. O Feign aposta na **convenção sobre configuração**: tudo funciona automaticamente com o mínimo de código. O Http Interface aposta na **explicitação**: o desenvolvedor configura cada detalhe, ganhando controle e eliminando dependências.

A escolha entre os dois não é técnica — é arquitetural. Em um ecossistema Spring Cloud com múltiplos microsserviços, o Feign é a escolha natural pela integração nativa com service discovery e resiliência. Em uma aplicação Spring Boot moderna sem Spring Cloud, o Http Interface é a escolha mais limpa, mais leve e mais alinhada com a direção do framework.

O projeto `http-interfaces` demonstra que as duas abordagens coexistem sem conflito no mesmo codebase, produzem o mesmo resultado funcional e diferem apenas na configuração e nas dependências que carregam. A decisão final pertence ao contexto — não à tecnologia.

---

## Referências

- [Spring Framework 6 — HTTP Interface Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)
- [Spring Cloud OpenFeign — Reference Documentation](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Spring Boot 4.1 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes)
- [RestClient — Spring Framework 6.1](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [Bucket4j — Token Bucket Algorithm](https://bucket4j.com/)
- [Liquibase — PreConditions](https://docs.liquibase.com/concepts/changelogs/preconditions.html)
- [ViaCep API](https://viacep.com.br)
