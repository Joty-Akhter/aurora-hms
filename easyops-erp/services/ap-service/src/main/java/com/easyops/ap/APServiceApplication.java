package com.easyops.ap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for Accounts Payable (AP) Service
 * Handles vendor management, bills, payments, and AP aging
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class APServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(APServiceApplication.class, args);
    }
}

