package com.easyops.hospitalcorporatediscount.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hospitalCorporateDiscountOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Corporate and Discount API")
                        .description("Corporate clients, contracts, coverage rules, and discount schemes for the Hospital Module.")
                        .version("1.0"));
    }
}
