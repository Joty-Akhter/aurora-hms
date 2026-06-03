package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Server-side Easy Prescription workspace: templates, EP config, recent Rx, draft blobs (JSON).
 * One row per (user, organization).
 */
@Entity
@Table(name = "ep_doctor_workspace", schema = "ehr",
        uniqueConstraints = @UniqueConstraint(name = "uq_ep_workspace_user_org", columnNames = {"user_id", "organization_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpDoctorWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "data_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String dataJson;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
