package com.easyops.rbac.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Role Entity
 * 
 * Represents the assignment of a role to a user.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_roles", schema = "rbac")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "organization_id")
    private UUID organizationId;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

