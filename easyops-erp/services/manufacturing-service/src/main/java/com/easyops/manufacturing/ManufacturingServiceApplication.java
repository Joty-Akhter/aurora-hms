package com.easyops.manufacturing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ManufacturingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManufacturingServiceApplication.class, args);
    }
}

