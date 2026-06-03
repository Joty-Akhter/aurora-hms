package com.easyops.accounting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8088}")
    private String serverPort;
    
    @Bean
    public OpenAPI accountingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Accounting Service API")
                        .description("Chart of Accounts, General Ledger, and Journal Entry Management for EasyOps ERP")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EasyOps Team")
                                .email("support@easyops.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local server"),
                        new Server().url("http://localhost:8081").description("API Gateway")
                ));
    }
}

