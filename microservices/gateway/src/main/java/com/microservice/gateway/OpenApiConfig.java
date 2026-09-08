package com.microservice.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private final String baseUrl;

    public OpenApiConfig(@Value("${api.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Bean
    public OpenAPI gatewayOpenApi() {
        OpenAPI openAPI = new OpenAPI()
        .components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("Gateway API")
                        .description("JWT authentication and routing entry point for the Laundry microservices example")
                        .version("v1"));

        // Always set the configured base URL if provided, this overrides auto-detected servers
        if (baseUrl != null && !baseUrl.isEmpty()) {
            openAPI.servers(java.util.List.of(new Server().url(baseUrl)));
        }

        return openAPI;
    }
}
