package com.easyops.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gateway Configuration
 * 
 * Configures the API Gateway routes and filters for the EasyOps ERP system.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Configuration
public class GatewayConfig {

    /**
     * Configure API Gateway routes
     * 
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Config (module config from DB) - also in user-management
                .route("config", r -> r.path("/api/config/**")
                        .uri("lb://user-management-service"))
                
                // User Management Service Routes
                .route("user-management", r -> r.path("/api/users/**")
                        .uri("lb://user-management-service"))
                
                // Authentication Service Routes
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))
                
                // RBAC Service Routes
                .route("rbac-service", r -> r.path("/api/rbac/**")
                        .uri("lb://rbac-service"))
                
                // Organization Service Routes
                .route("organization-service", r -> r.path("/api/organizations/**")
                        .uri("lb://organization-service"))
                
                // Accounting Service Routes
                .route("accounting-service", r -> r.path("/api/accounting/**")
                        .uri("lb://accounting-service"))
                
                // AR Service Routes
                .route("ar-service", r -> r.path("/api/ar/**")
                        .uri("lb://ar-service"))
                
                // AP Service Routes
                .route("ap-service", r -> r.path("/api/ap/**")
                        .uri("lb://ap-service"))
                
                // Bank Service Routes
                .route("bank-service", r -> r.path("/api/bank/**")
                        .uri("lb://bank-service"))
                
                // Notification Service Routes
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))
                
                // Monitoring Service Routes
                .route("monitoring-service", r -> r.path("/api/monitoring/**")
                        .uri("lb://monitoring-service"))
                
                // Health Check Routes
                .route("health-check", r -> r.path("/health/**")
                        .uri("lb://user-management-service"))
                
                .build();
    }

    /**
     * Load-balanced WebClient.Builder for internal service-to-service calls (e.g. API key validation).
     * The @LoadBalanced qualifier causes Spring Cloud LoadBalancer to apply the
     * ReactorLoadBalancerExchangeFilterFunction so that lb:// URIs are resolved via Eureka.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Configure CORS for API Gateway
     * 
     * @return CorsWebFilter
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        // When allowCredentials is true, must specify exact origins or use patterns without wildcard
        // Development
        corsConfig.addAllowedOriginPattern("http://localhost:*");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:*");
        // Production (each SPA origin that calls this gateway must be listed — browsers send Origin; curl does not)
        corsConfig.addAllowedOriginPattern("https://alien-erp.i2gether.com");
        corsConfig.addAllowedOriginPattern("https://aurora.i2gether.com");
        corsConfig.addAllowedOriginPattern("https://www.aurora.hospital");
        corsConfig.addAllowedOriginPattern("https://aurora.hospital");
        corsConfig.addAllowedOriginPattern("https://hms.aurora.hospital");
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
