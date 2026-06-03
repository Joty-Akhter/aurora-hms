package com.easyops.bank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI bankServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Service API")
                        .description("Bank and Cash Management Service for EasyOps ERP - Manages bank accounts, transactions, and reconciliation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EasyOps ERP")
                                .email("support@easyops.com")));
    }
}

