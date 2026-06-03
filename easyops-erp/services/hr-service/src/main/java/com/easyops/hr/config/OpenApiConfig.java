package com.easyops.hr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI hrServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HR Service API")
                        .description("Human Resources Management Service API for EasyOps ERP. " +
                                "Provides endpoints for Provident Fund management, Incentive calculations, " +
                                "Sales targets, and comprehensive reporting.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EasyOps Support")
                                .email("support@easyops.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://easyops.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.easyops.com/hr")
                                .description("Production Server")
                ));
    }
}

