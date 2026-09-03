package com.microservice.gateway;

import com.microservice.gateway.config.SecurityConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void corsConfigAllowsApiDomain() {
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/token");
        request.addHeader("Origin", "https://api.laundrywithme.com");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("https://api.laundrywithme.com");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void swaggerUsesHttpsPublicBaseUrl() {
        OpenApiConfig config = new OpenApiConfig("https://api.laundrywithme.com");

        OpenAPI openApi = config.gatewayOpenApi();

        assertThat(openApi.getServers()).isNotEmpty();
        assertThat(openApi.getServers().get(0).getUrl()).isEqualTo("https://api.laundrywithme.com");
    }
}
