package com.easyops.hospitalcorporatediscount.config;

import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RbacClientConfiguration {

    @Bean
    public RbacPermissionClient rbacPermissionClient(
            RestTemplate restTemplate,
            MeterRegistry meterRegistry,
            @Value("${spring.application.name:hospital-corporate-and-discount-service}") String applicationName,
            @Value("${services.rbac.base-url:http://localhost:8084}") String rbacBaseUrl) {
        return new RbacPermissionClient(restTemplate, meterRegistry, rbacBaseUrl, applicationName);
    }
}
