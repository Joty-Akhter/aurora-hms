package com.easyops.pharma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableJpaAuditing
@EnableFeignClients
public class PharmaServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PharmaServiceApplication.class, args);
    }
}

