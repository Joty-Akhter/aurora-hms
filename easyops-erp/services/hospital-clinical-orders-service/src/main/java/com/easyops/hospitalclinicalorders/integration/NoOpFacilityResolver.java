package com.easyops.hospitalclinicalorders.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * No-op facility resolver used when no facility master API is available.
 * Does not validate; any facility ID is accepted. Replace with a real implementation
 * (e.g. calling hospital-service or facility master) when validation is required.
 */
@Configuration
public class NoOpFacilityResolver {

    @Bean
    @ConditionalOnMissingBean(FacilityResolver.class)
    public FacilityResolver facilityResolver() {
        return new FacilityResolver() {
            @Override
            public void validate(UUID facilityId) {
                // No validation; facility_id is stored as provided.
            }
        };
    }
}
