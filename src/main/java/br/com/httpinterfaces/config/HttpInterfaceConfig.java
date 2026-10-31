package br.com.httpinterfaces.config;

import br.com.httpinterfaces.infrastructure.httpinterface.ViaCepHttpInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configura o proxy do Http Interface usando RestClient (síncrono).
 *
 * DIFERENÇA vs OpenFeign:
 * - OpenFeign: proxy criado automaticamente via @EnableFeignClients + @FeignClient
 * - Http Interface: proxy criado MANUALMENTE via HttpServiceProxyFactory
 * - Http Interface pode usar RestClient (síncrono) ou WebClient (reativo)
 * - Mais controle sobre timeouts, interceptors e serialização
 */
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
