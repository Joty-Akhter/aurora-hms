package com.easyops.hr.integration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * INT-40 / INT-42: Accept {@code X-Correlation-Id} or {@code X-Request-Id}; echo {@code X-Correlation-Id}
 * on every response; expose the value via {@link IntegrationCorrelationIdHolder} and {@code MDC} key {@code correlationId}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class IntegrationCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_CORRELATION = "X-Correlation-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final int MAX_LEN = 128;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String id = trimHeader(request.getHeader(HEADER_CORRELATION));
        if (id == null || id.isEmpty()) {
            id = trimHeader(request.getHeader(HEADER_REQUEST_ID));
        }
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        } else if (id.length() > MAX_LEN) {
            id = id.substring(0, MAX_LEN);
        }

        IntegrationCorrelationIdHolder.set(id);
        MDC.put("correlationId", id);
        response.setHeader(HEADER_CORRELATION, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            IntegrationCorrelationIdHolder.clear();
        }
    }

    private static String trimHeader(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
