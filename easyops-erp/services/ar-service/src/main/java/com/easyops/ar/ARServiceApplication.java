package com.easyops.ar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for Accounts Receivable (AR) Service
 * Handles customer management, invoicing, receipts, and AR aging
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class ARServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ARServiceApplication.class, args);
    }
}

