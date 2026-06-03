package com.easyops.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs every request at DEBUG for troubleshooting (path, method, Origin, Host, route).
 */
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String origin = request.getHeaders().getFirst("Origin");
        String host = request.getHeaders().getFirst("Host");
        String forwardedProto = request.getHeaders().getFirst("X-Forwarded-Proto");
        String routeId = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayPredicateRouteAttribute");

        log.debug("[Gateway] Incoming request: method={} path={} origin={} host={} x-forwarded-proto={} routeId={}",
                method, path, origin, host, forwardedProto, routeId);

        return chain.filter(exchange).doFinally(signal -> {
            if (log.isDebugEnabled()) {
                var statusCode = exchange.getResponse().getStatusCode();
                int status = statusCode != null ? statusCode.value() : -1;
                log.debug("[Gateway] Response: path={} status={}", path, status);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
