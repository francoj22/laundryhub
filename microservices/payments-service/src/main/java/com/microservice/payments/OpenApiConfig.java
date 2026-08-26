package com.microservice.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Processing API")
                        .description("Payment processing microservice with validation, idempotency, refunds, and webhook support")
                        .version("v1"));
    }
}
