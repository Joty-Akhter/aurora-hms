package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ep_doctor_workspace_audit", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpDoctorWorkspaceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_id")
    private UUID auditId;

    /** Nullable: READ audit rows may be recorded before any workspace row exists. */
    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
