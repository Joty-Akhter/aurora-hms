package com.easyops.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Organization Service Application
 * 
 * This service handles multi-tenancy and organization management
 * for the EasyOps ERP system.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableJpaRepositories
public class OrganizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }
}

