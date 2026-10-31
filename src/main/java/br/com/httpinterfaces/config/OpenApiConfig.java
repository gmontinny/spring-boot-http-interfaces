package br.com.httpinterfaces.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("HTTP Interfaces vs OpenFeign API")
                        .description("""
                                Projeto demonstrando a diferença entre **OpenFeign** e **Spring Http Interface**
                                integrados com ViaCep, autenticação JWT, Rate Limiting e importação de CSVs.
                                
                                - `/api/v1/addresses/feign/{cep}` — busca via **OpenFeign**
                                - `/api/v1/addresses/http-interface/{cep}` — busca via **Http Interface**
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("gmontinny").email("admin@httpinterfaces.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
