package com.microservice.gateway.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().equals("/api/payments/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for {}", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        Claims claims;
        try {
            String token = authHeader.substring(7);
            claims = jwtService.parseToken(token);
        } catch (Exception ex) {
            log.warn("JWT validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String userId = claims.getSubject();
        String role = String.valueOf(claims.get("role"));

        log.debug("JWT accepted for {} with userId={} role={}", request.getRequestURI(), userId, role);

        request.setAttribute("gatewayUserId", userId);
        request.setAttribute("gatewayUserRole", role);

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
