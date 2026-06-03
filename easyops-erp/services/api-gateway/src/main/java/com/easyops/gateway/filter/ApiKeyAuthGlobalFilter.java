package com.easyops.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Validates requests carrying an X-API-Key header.
 *
 * Flow:
 *  1. If no X-API-Key header is present the filter is a no-op (JWT or open route).
 *  2. The raw key is forwarded to auth-service POST /api/auth/apikey/validate.
 *  3. On success the gateway injects X-User-Id (and X-Organization-Id when present)
 *     into the downstream request, exactly matching what JWT auth would produce.
 *  4. On failure (unknown key, expired, inactive) the gateway returns 401 immediately.
 */
@Component
public class ApiKeyAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthGlobalFilter.class);
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ORG_ID  = "X-Organization-Id";

    private final WebClient webClient;

    @Value("${services.auth.base-url:lb://auth-service}")
    private String authServiceBaseUrl;

    public ApiKeyAuthGlobalFilter(@LoadBalanced WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawKey = exchange.getRequest().getHeaders().getFirst(HEADER_API_KEY);
        if (rawKey == null) {
            return chain.filter(exchange);
        }

        return webClient.post()
                .uri(authServiceBaseUrl + "/api/auth/apikey/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("apiKey", rawKey))
                .retrieve()
                .bodyToMono(ApiKeyValidateResponse.class)
                .flatMap(resp -> {
                    var builder = exchange.getRequest().mutate()
                            .header(HEADER_USER_ID, resp.getUserId().toString());
                    if (resp.getOrganizationId() != null) {
                        builder.header(HEADER_ORG_ID, resp.getOrganizationId().toString());
                    }
                    log.debug("[Gateway] API key authenticated: userId={} orgId={}",
                            resp.getUserId(), resp.getOrganizationId());
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .onErrorResume(ex -> {
                    log.warn("[Gateway] API key validation failed: {}", ex.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        // Run just after the logging filter (HIGHEST_PRECEDENCE) but before routing
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /** Local DTO — mirrors auth-service ApiKeyValidateResponse. */
    static class ApiKeyValidateResponse {
        private UUID userId;
        private UUID organizationId;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    }
}
