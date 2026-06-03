package com.easyops.ap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI apServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AP Service API")
                        .description("Accounts Payable Service for EasyOps ERP - Manages vendors, bills, payments, and aging reports")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EasyOps ERP")
                                .email("support@easyops.com")));
    }
}

