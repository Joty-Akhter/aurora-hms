package com.easyops.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application
 * 
 * This is the service discovery server for the EasyOps ERP system.
 * It enables service registration and discovery for all microservices.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
