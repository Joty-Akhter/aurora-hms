package com.easyops.hospital.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate that resolves {@code http://service-name/...} via Eureka + Spring Cloud LoadBalancer.
 * Used for hospital-service → hospital-card-management-service calls.
 */
@Configuration
public class LoadBalancedRestTemplateConfig {

    public static final String BEAN_NAME = "loadBalancedRestTemplate";

    @Bean(name = BEAN_NAME)
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
