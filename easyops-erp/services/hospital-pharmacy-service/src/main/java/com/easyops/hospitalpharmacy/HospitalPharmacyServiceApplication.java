package com.easyops.hospitalpharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.OffsetDateTime;
import java.util.Optional;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class HospitalPharmacyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalPharmacyServiceApplication.class, args);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}

