package br.com.httpinterfaces.infrastructure.httpinterface;

import br.com.httpinterfaces.application.dto.ViaCepResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Spring 6 Http Interface Client para ViaCep.
 *
 * DIFERENÇA vs OpenFeign:
 * - Nativo do Spring Framework 6+ (zero dependência extra)
 * - Usa @HttpExchange / @GetExchange em vez de @FeignClient / @GetMapping
 * - Proxy criado via HttpServiceProxyFactory + WebClient ou RestClient
 * - Suporte reativo (WebClient) e síncrono (RestClient) no mesmo contrato
 * - Mais leve: sem Spring Cloud, sem processamento de anotações externo
 * - Ideal para Spring Boot 3+ / 4+ sem overhead de Spring Cloud
 * - Configuração manual do proxy (ver HttpInterfaceConfig)
 */
@HttpExchange(url = "/ws")
public interface ViaCepHttpInterface {

    @GetExchange("/{cep}/json/")
    ViaCepResponse findByCep(@PathVariable String cep);
}
