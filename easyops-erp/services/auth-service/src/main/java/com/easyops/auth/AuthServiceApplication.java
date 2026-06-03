package com.easyops.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Authentication Service Application
 * 
 * This service handles user authentication, JWT token generation,
 * session management, and password operations for the EasyOps ERP system.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

