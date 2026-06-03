package com.easyops.ar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI arServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AR Service API")
                        .description("Accounts Receivable Service for EasyOps ERP - Manages customers, invoices, receipts, and aging reports")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EasyOps ERP")
                                .email("support@easyops.com")));
    }
}

