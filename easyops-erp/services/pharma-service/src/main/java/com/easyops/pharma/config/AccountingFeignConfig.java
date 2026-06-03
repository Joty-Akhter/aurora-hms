package com.easyops.pharma.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates {@code X-User-Id} to accounting-service for journal integration RBAC.
 */
@Configuration
public class AccountingFeignConfig {

    @Bean
    public RequestInterceptor accountingServiceHeaderPropagation(
            @Value("${integration.accounting.system-user-id:00000000-0000-0000-0000-000000000001}") String systemUserId) {
        return requestTemplate -> {
            String userId = null;
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest req = servletRequestAttributes.getRequest();
                userId = req.getHeader("X-User-Id");
            }
            if (userId == null || userId.isBlank()) {
                userId = systemUserId;
            }
            requestTemplate.header("X-User-Id", userId);
        };
    }
}
