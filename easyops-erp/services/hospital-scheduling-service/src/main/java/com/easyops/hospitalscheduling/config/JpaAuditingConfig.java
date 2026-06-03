package com.easyops.hospitalscheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA auditing: provides current user for created_by when available.
 * When platform RBAC/auth is integrated, replace this with an implementation
 * that returns the authenticated user's ID from SecurityContext or similar.
 */
@Configuration
public class JpaAuditingConfig {

    /**
     * Default Spring Data auditing supplies {@link java.time.LocalDateTime}, which cannot be assigned to
     * {@link OffsetDateTime} entity fields (e.g. {@code created_at} on {@code WorkingHours}). That broke
     * POST /resources/{id}/working-hours when hospital-service syncs doctor slots.
     */
    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.empty();
    }
}
