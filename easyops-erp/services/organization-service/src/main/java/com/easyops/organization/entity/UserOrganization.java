package com.easyops.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User Organization Entity
 * Maps users to organizations with roles and permissions
 */
@Entity
@Table(name = "user_organizations", schema = "admin",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 50)
    private String role = "MEMBER";

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "joined_at")
    private ZonedDateTime joinedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(length = 50)
    private String status = "ACTIVE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private Map<String, Object> permissions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}

