# http-interfaces

Projeto Spring Boot demonstrando e comparando **OpenFeign** (Spring Cloud) vs **Http Interface** (Spring 6 nativo) na integração com a API pública [ViaCep](https://viacep.com.br). Inclui autenticação JWT, rate limiting, importação de CSVs de marketplaces e documentação Swagger.

---

## Objetivo Principal

Evidenciar as diferenças práticas entre as duas abordagens de cliente HTTP declarativo:

| Aspecto | OpenFeign | Http Interface |
|---|---|---|
| Origem | Spring Cloud | Spring Framework 6+ (nativo) |
| Anotação de interface | `@FeignClient` | `@HttpExchange` |
| Anotações de método | `@GetMapping`, `@PostMapping`... | `@GetExchange`, `@PostExchange`... |
| Criação do proxy | Automática via `@EnableFeignClients` | Manual via `HttpServiceProxyFactory` |
| Transporte | Feign HTTP client | `RestClient` (síncrono) ou `WebClient` (reativo) |
| Dependência extra | `spring-cloud-starter-openfeign` | Nenhuma — já incluso no Spring |
| Circuit breaker / Retry | Nativo via Resilience4j | Configuração manual |
| Ideal para | Microsserviços com Spring Cloud | Spring Boot 3+/4+ sem overhead de Cloud |

O campo `fetchedVia` na entidade `Address` registra qual cliente foi usado (`FEIGN` ou `HTTP_INTERFACE`), tornando a diferença visível nos dados persistidos.

---

## Stack

- **Java 25**
- **Spring Boot 4.1.2**
- **Spring Cloud 2025.1.4** (OpenFeign)
- **PostgreSQL 16** via Docker
- **Liquibase** — migrações de schema
- **JWT** (jjwt 0.12.6) — autenticação stateless
- **Bucket4j 8.10.1** — rate limiting por IP
- **Springdoc OpenAPI 3.1.0** — Swagger UI
- **OpenCSV 5.9** — importação de CSVs
- **HashiCorp Vault 1.17** — gerenciamento de secrets (opcional)
- **Lombok**

---

## Estrutura do Projeto

```
src/main/java/br/com/httpinterfaces/
├── application/
│   ├── dto/                    # DTOs de request/response
│   │   ├── UserRequest.java    # Criação/atualização de usuário
│   │   └── UserResponse.java   # Resposta sem expor senha
│   └── mapper/                 # Mapeadores manuais (Address, Product, User)
├── config/
│   ├── DataLoader              # Importação automática dos CSVs na inicialização
│   ├── HttpInterfaceConfig     # Configura proxy do Http Interface manualmente
│   └── OpenApiConfig           # Swagger com Bearer JWT
├── domain/
│   ├── model/                  # Entidades JPA (User, Role, RefreshToken, Product, Address)
│   ├── repository/             # Repositórios Spring Data JPA
│   └── service/                # Regras de negócio (Auth, Address, Product, CsvImport, User)
├── infrastructure/
│   ├── feign/                  # ViaCepFeignClient — cliente OpenFeign
│   ├── httpinterface/          # ViaCepHttpInterface — cliente Http Interface
│   ├── ratelimit/              # RateLimitService + RateLimitFilter (Bucket4j)
│   └── security/               # JwtService, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl
└── presentation/
    ├── controller/             # AuthController, AddressController, ProductController, ImportController, UserController
    └── exception/              # GlobalExceptionHandler
```

---

## Pré-requisitos

- Java 25+
- Docker e Docker Compose
- Gradle (ou use o wrapper `./gradlew`)

---

## Como Executar

**1. Subir a infraestrutura (PostgreSQL + Vault):**

```bash
docker-compose up -d
```

**2. Executar a aplicação:**

```bash
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080`.

Na primeira inicialização, o `DataLoader` detecta que a tabela `products` está vazia e importa automaticamente todos os CSVs da pasta `data/`:

```
Imported 1001 products from data/amazon-products.csv  (source=amazon)
Imported 22429 products from data/shopee-products.csv (source=shopee)
Imported 1000 products from data/lazada-products.csv  (source=lazada)
Imported 1000 products from data/shein-products.csv   (source=shein)
Imported 1011 products from data/walmart-products.csv (source=walmart)
CSV import complete. Total products imported: ~26441
```

Nas inicializações seguintes, o `DataLoader` detecta que já existem registros e pula a importação.

**3. Acessar o Swagger UI:**

```
http://localhost:8080/swagger-ui.html
```

---

## Variáveis de Configuração (`application.yml`)

| Propriedade | Padrão | Descrição |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/ecommerce` | URL do banco |
| `spring.datasource.username` | `gmontinny` | Usuário do banco |
| `spring.datasource.password` | `Gmontinny2026` | Senha do banco |
| `app.jwt.secret` | (hex 64 chars) | Chave HMAC para assinar tokens |
| `app.jwt.expiration` | `86400000` (24h) | Validade do access token em ms |
| `app.jwt.refresh-expiration` | `604800000` (7d) | Validade do refresh token em ms |
| `viacep.base-url` | `https://viacep.com.br` | Base URL da API ViaCep |
| `spring.cloud.vault.enabled` | `false` | Habilitar integração com Vault |

> Em produção, sobrescreva `app.jwt.secret` e credenciais do banco via Vault ou variáveis de ambiente.

---

## Endpoints

### Autenticação — `/api/v1/auth`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/register` | Registrar novo usuário |
| `POST` | `/login` | Login — retorna `accessToken` + `refreshToken` |
| `POST` | `/refresh` | Renovar access token via refresh token |
| `POST` | `/logout` | Invalidar refresh token (requer JWT) |

### Usuários — `/api/v1/users` *(requer `ADMIN`)*

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Lista todos os usuários |
| `GET` | `/{id}` | Busca usuário por ID |
| `POST` | `/` | Cria usuário com roles customizadas |
| `PUT` | `/{id}` | Atualiza dados e roles do usuário |
| `DELETE` | `/{id}` | Remove usuário |

O campo `roles` no body é opcional — se omitido, atribui `ROLE_USER` por padrão.

Exemplo de body para criar um admin:
```json
{
  "username": "admin",
  "email": "admin@example.com",
  "password": "senha123",
  "roles": ["ROLE_ADMIN"]
}
```

### Endereços — `/api/v1/addresses`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/feign/{cep}` | Busca CEP via **OpenFeign**, persiste com `fetchedVia=FEIGN` |
| `GET` | `/http-interface/{cep}` | Busca CEP via **Http Interface**, persiste com `fetchedVia=HTTP_INTERFACE` |
| `GET` | `/` | Lista todos os endereços salvos |
| `GET` | `/{id}` | Busca endereço por ID |
| `GET` | `/cep/{cep}` | Filtra endereços por CEP |
| `GET` | `/source/{fetchedVia}` | Filtra por cliente usado (`FEIGN` ou `HTTP_INTERFACE`) |
| `DELETE` | `/{id}` | Remove endereço (requer JWT) |

### Produtos — `/api/v1/products`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Lista paginada de produtos |
| `GET` | `/{id}` | Busca produto por ID |
| `GET` | `/source/{source}` | Filtra por marketplace (amazon, shopee, lazada, shein, walmart) |
| `GET` | `/brand/{brand}` | Filtra por marca |
| `GET` | `/search?keyword=` | Busca por palavra-chave no título |
| `POST` | `/` | Cria produto (requer `ADMIN`) |
| `PUT` | `/{id}` | Atualiza produto (requer `ADMIN`) |
| `DELETE` | `/{id}` | Remove produto (requer `ADMIN`) |

### Importação CSV — `/api/v1/import` *(requer `ADMIN`)*

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/products` | Upload de CSV + param `source` |

> A importação também ocorre **automaticamente** na inicialização via `DataLoader` se a tabela estiver vazia. O endpoint manual permite reimportar ou adicionar novos arquivos.

---

## ETL — Importação dos CSVs

### Arquivos disponíveis em `data/`

| Arquivo | Source | Linhas (~produtos) |
|---|---|---|
| `amazon-products.csv` | `amazon` | ~1.001 |
| `shopee-products.csv` | `shopee` | ~22.429 |
| `lazada-products.csv` | `lazada` | ~1.000 |
| `shein-products.csv` | `shein` | ~1.000 |
| `walmart-products.csv` | `walmart` | ~1.011 |

### Mapeamento de campos por marketplace

Cada CSV usa nomes de colunas diferentes. O `CsvImportService` resolve isso com aliases em ordem de prioridade (`firstNonNull`):

| Campo da entidade | Amazon | Shopee | Lazada | Shein | Walmart |
|---|---|---|---|---|---|
| `title` | `title` | `title` | `title` | `product_name` | `product_name` |
| `description` | `description` | — | `product_description` | `description` | `description` |
| `initialPrice` | `initial_price` | `initial_price` | `initial_price` | `initial_price` | `initial_price` |
| `finalPrice` | `final_price` | `final_price` | `final_price` | `final_price` | `final_price` |
| `currency` | `currency` | `currency` | `currency` | `currency` | `currency` |
| `rating` | `rating` | `rating` | `rating` | `rating` | `rating` |
| `reviewsCount` | `reviews_count` | `reviews` | `reviews` | `reviews_count` | `review_count` |
| `imageUrl` | `image_url` | `image` | `image` | `main_image` | `main_image` |
| `availability` | `availability` | `stock` | — | `in_stock` | — |
| `categories` | `categories` | `breadcrumb` | `breadcrumb` | `category_tree` | — |
| `externalId` | `asin` | `id` | `sku` | `product_id` | `sku` |
| `sellerName` | `seller_name` | `seller_name` | `seller_name` | — | `seller` |
| `brand` | `brand` | `brand` | `brand` | `brand` | `brand` |

### Importação manual via Swagger

1. Acesse `http://localhost:8080/swagger-ui.html`
2. Autentique-se com um usuário `ADMIN` via `POST /api/v1/auth/login`
3. Clique em **Authorize** e cole o `accessToken`
4. Use `POST /api/v1/import/products` com o arquivo CSV e o parâmetro `source`

### Importação manual via curl

```bash
# Login para obter token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"senha123"}' | jq -r '.accessToken')

# Importar cada marketplace
curl -X POST "http://localhost:8080/api/v1/import/products?source=amazon" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@data/amazon-products.csv"

curl -X POST "http://localhost:8080/api/v1/import/products?source=shopee" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@data/shopee-products.csv"

curl -X POST "http://localhost:8080/api/v1/import/products?source=lazada" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@data/lazada-products.csv"

curl -X POST "http://localhost:8080/api/v1/import/products?source=shein" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@data/shein-products.csv"

curl -X POST "http://localhost:8080/api/v1/import/products?source=walmart" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@data/walmart-products.csv"
```

---

## Segurança

- Autenticação **stateless** com JWT (Bearer Token)
- Rotas públicas: `POST /auth/**`, `GET /products/**`, `/swagger-ui/**`, `/api-docs/**`
- Rotas de escrita (`POST`, `PUT`, `DELETE`) exigem role `ADMIN`
- Rotas `/api/v1/users/**` exigem role `ADMIN`
- **Rate Limiting**: 20 requisições/minuto por IP via Bucket4j (ignora rotas Swagger)
- Refresh token com **rotação** — cada uso gera um novo token e invalida o anterior

---

## Banco de Dados

Migrações gerenciadas pelo **Liquibase** em `src/main/resources/db/changelog/`:

| Arquivo | O que faz |
|---|---|
| `001-create-users-roles.xml` | Cria `users`, `roles`, `user_roles`, `refresh_tokens` |
| `002-create-products.xml` | Cria `products` |
| `003-create-addresses.xml` | Cria `addresses` |
| `004-alter-products-rating.xml` | Altera `rating` de `NUMERIC(3,1)` → `NUMERIC(4,2)` |
| `005-grant-admin-to-gmontinny.xml` | Concede `ROLE_ADMIN` ao usuário `gmontinny` |

---

## Lições Aprendidas — Erros de ETL

Esta seção documenta os erros reais encontrados durante a importação dos CSVs e como foram resolvidos. Serve de referência para projetos futuros com dados heterogêneos de múltiplas fontes.

### Erro 1 — `CsvMalformedLineException` (Shopee, Lazada, Walmart)

**Sintoma:**
```
CsvMalformedLineException: Delimitador de fim de campo texto não encontrado ao final da linha CSV.
Começo do texto perdido: ("value":null}],0,https://shopee.com.co/...
```

**Causa:** Campos que contêm JSON embutido com quebras de linha dentro das aspas. O `CSVReader.readAll()` tentava carregar o arquivo inteiro na memória e abortava completamente ao encontrar a primeira linha malformada.

**Solução:** Substituir `readAll()` por leitura linha a linha com `reader.readNext()` dentro de um `try/catch` por linha (`safeReadNext`). Linhas malformadas são logadas como `WARN` e puladas individualmente — o restante do arquivo continua sendo processado normalmente.

```java
// Antes — falha no arquivo inteiro
List<String[]> rows = reader.readAll();

// Depois — falha apenas na linha problemática
String[] row;
while ((row = safeReadNext(reader, lineNum)) != null) { ... }
```

**Lição:** Nunca use `readAll()` em CSVs de origem externa. Sempre leia linha a linha com tratamento individual de exceções.

---

### Erro 2 — `NullPointerException` em `getColumn` (Shopee, Lazada)

**Sintoma:**
```
Skipping row 102 due to parse error: Cannot invoke "String.trim()" because "row[i]" is null
```

**Causa:** O `CSVParserBuilder` configurado com `withFieldAsNull(EMPTY_SEPARATORS)` retorna `null` para células vazias em vez de `""`. O código fazia `row[i].trim()` sem verificar null.

**Solução:** Adicionar guard antes do `.trim()`:

```java
// Antes
String val = row[i].trim();

// Depois
String raw = row[i];
if (raw == null) return null;
String val = raw.trim();
```

**Lição:** Ao configurar o parser CSV com `withFieldAsNull`, todos os acessos a células devem checar null explicitamente.

---

### Erro 3 — `numeric field overflow` no campo `rating` (Shein)

**Sintoma:**
```
ERROR: numeric field overflow
Detalhe: A field with precision 3, scale 1 must round to an absolute value less than 10^2.
```

**Causa:** O campo `rating` estava definido como `NUMERIC(3,1)` (máximo `9.9`). O `parseBigDecimal` usava `replaceAll("[^0-9.]", "")` que transformava valores como `"4.5 out of 5"` em ``"45"` — um número fora do range.

**Solução em duas partes:**
1. Migration `004` altera a coluna para `NUMERIC(4,2)` — suporta até `99.99`
2. Método `parseRating` descarta qualquer valor `>= 10` ou `< 0`:

```java
private BigDecimal parseRating(String value) {
    BigDecimal rating = parseBigDecimal(value);
    if (rating == null) return null;
    if (rating.compareTo(BigDecimal.TEN) >= 0) return null; // descarta lixo
    if (rating.compareTo(BigDecimal.ZERO) < 0) return null;
    return rating;
}
```

**Lição:** Sempre valide ranges de campos numéricos antes de persistir. Defina a precisão do banco com margem — `NUMERIC(3,1)` para rating é muito restrito.

---

### Erro 4 — `numeric field overflow` no campo `initial_price` / `final_price` (Shopee)

**Sintoma:**
```
ERROR: numeric field overflow
Detalhe: A field with precision 15, scale 2 must round to an absolute value less than 10^13.
```

**Causa:** O Shopee armazena preços em notação científica: `"1.580950000000000e+03"`. O `replaceAll("[^0-9.]", "")` transformava isso em `"1.58095000000000003"` — um número com 17 dígitos significativos que estoura `NUMERIC(15,2)`.

**Solução:** Usar `new BigDecimal(value.trim())` diretamente, que entende notação científica nativamente. Fallback com `replaceAll` apenas para valores com símbolos como `"$12.99"`:

```java
// Antes — quebrava com notação científica
new BigDecimal(value.replaceAll("[^0-9.]", ""))

// Depois — entende "1.58e+03" corretamente
new BigDecimal(value.trim())  // tenta primeiro
// fallback: replaceAll para "$12.99" → "12.99"
```

Além disso, `parsePrice` descarta valores absurdos (`> 9_999_999_999_999`) e aplica `setScale(2, HALF_UP)`.

**Lição:** Dados de marketplaces internacionais frequentemente usam notação científica para preços. `new BigDecimal(String)` é mais robusto que `replaceAll` para parsing numérico.

---

### Erro 5 — `DaoAuthenticationProvider()` no-arg constructor removido (Spring Boot 4)

**Sintoma:**
```
NoSuchMethodError: DaoAuthenticationProvider.<init>()
```

**Causa:** Spring Boot 4 / Spring Security 7 removeu o construtor sem argumentos de `DaoAuthenticationProvider`. O `UserDetailsService` agora é obrigatório no construtor.

**Solução:**
```java
// Antes (Spring Boot 3)
var provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService);

// Depois (Spring Boot 4)
var provider = new DaoAuthenticationProvider(userDetailsService);
```

**Lição:** Ao migrar para Spring Boot 4, revisar todos os usos de `DaoAuthenticationProvider`. O construtor com `UserDetailsService` é a única forma suportada.

---

### Erro 6 — `ClassNotFoundException: WebMvcProperties` (Springdoc 2.x)

**Sintoma:**
```
ClassNotFoundException: org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties
```

**Causa:** `springdoc-openapi 2.x` referencia `WebMvcProperties` que foi movida/removida no Spring Boot 4 / Spring Framework 7.

**Solução:** Atualizar para `springdoc-openapi-starter-webmvc-ui:3.1.0`, que é compatível com Spring Boot 4.

**Lição:** Springdoc 2.x é compatível apenas com Spring Boot 2.x/3.x. Para Spring Boot 4, usar `3.1.0+`.

---

### Erro 8 — 403 Forbidden após login com usuário existente

**Sintoma:**
```
HTTP 403 Forbidden em GET /api/v1/users com token JWT válido
```

**Causa:** O endpoint `/auth/register` sempre atribui `ROLE_USER` — nunca `ROLE_ADMIN`. O usuário `gmontinny` foi criado via `/register` e tinha apenas `ROLE_USER`. O JWT é stateless: as authorities são carregadas do banco no momento do login, então um token gerado antes da promoção não reflete a nova role.

**Solução errada (evitar):** Executar SQL diretamente no banco (`INSERT INTO user_roles ...`). Bypassa o versionamento e não é rastreável.

**Solução correta:** Migration Liquibase `005-grant-admin-to-gmontinny.xml` com `preConditions onFail="MARK_RAN"` — executada automaticamente na próxima inicialização:

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

Após reiniciar a aplicação, fazer login novamente para obter um token com `ROLE_ADMIN` nas authorities.

**Lição:** Alterações de dados de segurança (roles, permissões) devem sempre ser versionadas via Liquibase. SQL direto no banco não é rastreável, não é reproduzível em outros ambientes e quebra o histórico de migrações.

---

### Erro 7 — Banco criado com nome errado (typo no Docker)

**Sintoma:**
```
Failed to obtain JDBC Connection: FATAL: database "ecommece" does not exist
```

**Causa:** O `docker-compose.yml` tinha `POSTGRES_DB: ecommece` (faltava o `r`). O container foi criado com o nome errado e `docker-compose down` sem `-v` não remove volumes — o banco persistiu com o typo.

**Solução:**
```bash
docker-compose down -v  # remove volumes
docker-compose up -d    # recria com nome correto
```

**Lição:** Sempre use `docker-compose down -v` para recriar o banco do zero. Sem `-v`, o volume persiste mesmo após `down`.

---

## Como Usar o Vault (Opcional)

O Vault está configurado em modo dev. Para habilitar:

```yaml
spring:
  cloud:
    vault:
      enabled: true
```

Adicione os secrets no Vault:

```bash
vault kv put secret/http-interfaces \
  app.jwt.secret=<seu-secret> \
  spring.datasource.password=<sua-senha>
```

---

## Diferença Central — Código

**OpenFeign** — proxy automático:
```java
@FeignClient(name = "viacep-feign", url = "${viacep.base-url}")
public interface ViaCepFeignClient {
    @GetMapping("/ws/{cep}/json/")
    ViaCepResponse findByCep(@PathVariable("cep") String cep);
}
```

**Http Interface** — proxy manual via `HttpServiceProxyFactory`:
```java
@HttpExchange(url = "/ws")
public interface ViaCepHttpInterface {
    @GetExchange("/{cep}/json/")
    ViaCepResponse findByCep(String cep);
}

// HttpInterfaceConfig.java
@Bean
public ViaCepHttpInterface viaCepHttpInterface() {
    RestClient restClient = RestClient.builder().baseUrl(viaCepBaseUrl).build();
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient)).build();
    return factory.createClient(ViaCepHttpInterface.class);
}
```

---

## Docker Compose

```bash
# Subir tudo
docker-compose up -d

# Parar
docker-compose down

# Parar e remover volumes (recriar banco do zero)
docker-compose down -v
```

Serviços:
- **PostgreSQL**: `localhost:5432` — banco `ecommerce`
- **Vault**: `localhost:8200` — token `myroot`
