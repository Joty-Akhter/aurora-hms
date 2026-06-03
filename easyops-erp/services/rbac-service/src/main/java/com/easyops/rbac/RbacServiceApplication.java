package com.easyops.rbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * RBAC Service Application
 * 
 * This service handles role-based access control, including role management,
 * permission management, and authorization checks for the EasyOps ERP system.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class RbacServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacServiceApplication.class, args);
    }
}

