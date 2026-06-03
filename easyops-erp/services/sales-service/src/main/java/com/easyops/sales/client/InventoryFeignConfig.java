package com.easyops.sales.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propagates {@code X-User-Id} / {@code X-Organization-Id} to inventory-service when the sales API
 * was invoked in a servlet request (matches inventory Phase 2 RBAC headers).
 */
@Configuration
public class InventoryFeignConfig {

    @Bean
    public RequestInterceptor inventoryServiceHeaderPropagation() {
        return requestTemplate -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest req = attrs.getRequest();
            String userId = req.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                requestTemplate.header("X-User-Id", userId);
            }
            String orgId = req.getHeader("X-Organization-Id");
            if (orgId != null && !orgId.isBlank()) {
                requestTemplate.header("X-Organization-Id", orgId);
            }
        };
    }
}
