package com.easyops.hr.config;

import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RbacClientConfiguration {

    @Bean
    @Qualifier("rbacRestTemplate")
    public RestTemplate rbacRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RbacPermissionClient rbacPermissionClient(
            @Qualifier("rbacRestTemplate") RestTemplate rbacRestTemplate,
            MeterRegistry meterRegistry,
            @Value("${spring.application.name:hr-service}") String applicationName,
            @Value("${services.rbac.base-url:http://localhost:8084}") String rbacBaseUrl) {
        return new RbacPermissionClient(rbacRestTemplate, meterRegistry, rbacBaseUrl, applicationName);
    }
}
