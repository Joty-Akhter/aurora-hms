package com.easyops.manufacturing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bom_versions", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomVersion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "version_id", updatable = false, nullable = false)
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomHeader bomHeader;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    // Version Info
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "revision", length = 20)
    private String revision;

    // Change Management
    @Column(name = "change_type", length = 50)
    private String changeType; // MINOR, MAJOR, ECN, ECO

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    // Approval
    @Column(name = "status", length = 50)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    // Metadata
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

