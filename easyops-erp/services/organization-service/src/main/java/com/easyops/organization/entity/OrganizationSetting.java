package com.easyops.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Organization Setting Entity
 * Stores tenant-specific configuration settings
 */
@Entity
@Table(name = "organization_settings", schema = "admin",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "setting_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_type", length = 50)
    private String settingType;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}

