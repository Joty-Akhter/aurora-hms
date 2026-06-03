package com.easyops.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application
 * 
 * This is the API Gateway service for the EasyOps ERP system.
 * It provides routing, load balancing, security, and monitoring
 * for all microservices in the system.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
