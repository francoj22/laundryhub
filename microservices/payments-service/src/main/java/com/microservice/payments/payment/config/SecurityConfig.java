package com.microservice.payments.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain paymentsFilterChain(HttpSecurity http, HeaderAuthenticationFilter headerAuthenticationFilter) throws Exception {
        http
                .securityMatcher("/payments/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }
}

class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/payments/webhook")
                || request.getRequestURI().startsWith("/swagger-ui")
                || request.getRequestURI().startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        if (userId == null || userId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + (role == null || role.isBlank() ? "USER" : role.toUpperCase())))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
