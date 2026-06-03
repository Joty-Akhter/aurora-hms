package com.easyops.hospitalcard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hospitalCardManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Card Management API")
                        .description("Card and wallet operations: products, limit profiles, cards, balances, top-up and adjustments.")
                        .version("1.0"));
    }
}
