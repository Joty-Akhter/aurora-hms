package com.easyops.hospitalpharmacy.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PharmacyDispenseProperties.class,
        PharmacyIntegrationProperties.class,
        PharmacyRegionalProperties.class
})
public class PharmacyDispenseConfiguration {
}
