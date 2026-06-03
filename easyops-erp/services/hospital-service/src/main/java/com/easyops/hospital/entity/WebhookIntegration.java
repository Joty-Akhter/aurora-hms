package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR-P3.11a — Per-pharmacy-integration shared secret for fill-status webhook HMAC-SHA256 auth.
 *
 * <p>Each calling system (pharmacy network, Surescripts relay, etc.) is identified by
 * {@code integrationId} supplied in the {@code X-Webhook-Integration-Id} request header.
 * The {@code secret} field holds the shared HMAC key.  In production this should be
 * encrypted at the application layer with an AES-256 key sourced from a secrets manager.
 */
@Entity
@Table(name = "webhook_integrations", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookIntegration {

    @Id
    @Column(name = "integration_id")
    private UUID integrationId;

    @Column(name = "integration_name", nullable = false, length = 100)
    private String integrationName;

    @Column(name = "integration_type", nullable = false, length = 50)
    @Builder.Default
    private String integrationType = "FILL_STATUS_CALLBACK";

    /** Shared HMAC secret — never expose in API responses or logs. */
    @Column(name = "secret", nullable = false, columnDefinition = "TEXT")
    private String secret;

    /** Optional comma-separated CIDR IP allowlist (secondary control). */
    @Column(name = "allowed_ips", columnDefinition = "TEXT")
    private String allowedIps;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private UUID createdBy;
}
