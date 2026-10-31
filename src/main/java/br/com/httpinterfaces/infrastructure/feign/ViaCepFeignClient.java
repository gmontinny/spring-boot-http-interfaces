package br.com.httpinterfaces.infrastructure.feign;

import br.com.httpinterfaces.application.dto.ViaCepResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign Client para ViaCep.
 *
 * DIFERENÇA vs Http Interface:
 * - Requer @FeignClient com nome e url
 * - Usa anotações Spring MVC normalmente
 * - Configurado via Spring Cloud OpenFeign (dependência extra)
 * - Suporta fallback, retry, circuit breaker nativamente via Resilience4j
 * - Geração de proxy em tempo de compilação via processamento de anotações
 * - Mais maduro, amplamente usado em microsserviços
 */
@FeignClient(name = "viacep-feign", url = "${viacep.base-url}")
public interface ViaCepFeignClient {

    @GetMapping("/ws/{cep}/json/")
    ViaCepResponse findByCep(@PathVariable("cep") String cep);
}
