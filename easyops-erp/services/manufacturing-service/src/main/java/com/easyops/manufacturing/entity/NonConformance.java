package com.easyops.manufacturing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "non_conformances", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NonConformance {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "nc_id", updatable = false, nullable = false)
    private UUID ncId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "nc_number", nullable = false, length = 50)
    private String ncNumber;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "inspection_id")
    private UUID inspectionId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "nc_type", nullable = false, length = 50)
    private String ncType;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "quantity_affected", precision = 15, scale = 3)
    private BigDecimal quantityAffected;

    @Column(name = "quantity_reworked", precision = 15, scale = 3)
    private BigDecimal quantityReworked;

    @Column(name = "quantity_scrapped", precision = 15, scale = 3)
    private BigDecimal quantityScrapped;

    @Column(name = "root_cause", length = 50)
    private String rootCause;

    @Column(name = "root_cause_description", columnDefinition = "TEXT")
    private String rootCauseDescription;

    @Column(name = "immediate_action", columnDefinition = "TEXT")
    private String immediateAction;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "preventive_action", columnDefinition = "TEXT")
    private String preventiveAction;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @Column(name = "reported_date")
    private LocalDateTime reportedDate;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "disposition", length = 50)
    private String disposition;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;

    @Column(name = "effectiveness_check")
    private Boolean effectivenessCheck;

    @Column(name = "cost_impact", precision = 15, scale = 2)
    private BigDecimal costImpact;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reportedDate == null) reportedDate = LocalDateTime.now();
        if (effectivenessCheck == null) effectivenessCheck = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

