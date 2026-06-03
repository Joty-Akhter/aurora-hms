package com.easyops.hospitalscheduling.integration;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

/**
 * Feign configuration for {@link HospitalPatientClient}: forwards {@code X-User-Id} to hospital-service when
 * {@code hospital.integration.patient-lookup.outbound-user-id} is set so GET patient can resolve identity card fields.
 */
@Slf4j
public class HospitalPatientClientConfiguration {

    @Bean
    public RequestInterceptor hospitalPatientOutboundUserIdInterceptor(
            @Value("${hospital.integration.patient-lookup.outbound-user-id:}") String outboundUserId) {
        return requestTemplate -> {
            if (outboundUserId == null || outboundUserId.isBlank()) {
                return;
            }
            String trimmed = outboundUserId.trim();
            try {
                UUID.fromString(trimmed);
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Skipping X-User-Id on hospital patient Feign call: invalid hospital.integration.patient-lookup.outbound-user-id");
                return;
            }
            requestTemplate.header("X-User-Id", trimmed);
        };
    }
}
