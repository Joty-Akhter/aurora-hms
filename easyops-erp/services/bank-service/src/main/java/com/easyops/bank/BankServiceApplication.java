package com.easyops.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for Bank Service
 * Handles bank accounts, transactions, and reconciliation
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class BankServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BankServiceApplication.class, args);
    }
}

