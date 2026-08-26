package com.microservice.gateway.config;

import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
        .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
    http
        .securityMatcher("/auth/token", "/api/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/token").permitAll()
            .requestMatchers("/api/payments/webhook").permitAll()
            .requestMatchers("/api/**").authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
    }
}
