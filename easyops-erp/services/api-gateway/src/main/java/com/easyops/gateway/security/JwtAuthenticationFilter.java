package com.easyops.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter
 * 
 * This filter validates JWT tokens for API Gateway requests.
 * It extracts the token from the Authorization header and validates it.
 * 
 * TEMPORARILY DISABLED FOR DEVELOPMENT
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
// @Component  // DISABLED - No JWT validation in development
public class JwtAuthenticationFilter implements GlobalFilter {

    @Value("${jwt.secret:your-jwt-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TEMPORARY: Bypass all JWT validation for development
        // TODO: Re-enable JWT validation once secret synchronization is fixed
        System.out.println("JwtAuthenticationFilter: BYPASSING all auth checks (dev mode)");
        return chain.filter(exchange);
    }

    /**
     * Check if the endpoint is public (doesn't require authentication)
     * 
     * @param path Request path
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        List<String> publicEndpoints = List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/auth/health",
                "/api/auth/password/reset",
                "/api/auth/password/reset/options",
                "/api/auth/password/reset/confirm",
                "/api/auth/generate-hash",
                "/api/public/web-booking",
                "/api/users",  // Temporarily public for testing
                "/health",
                "/actuator/health",
                "/actuator/info"
        );
        
        return publicEndpoints.stream().anyMatch(path::startsWith);
    }

    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return Claims if valid, null otherwise
     */
    private Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
