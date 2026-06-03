package com.easyops.manufacturing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quality_inspections", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityInspection {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "inspection_id", updatable = false, nullable = false)
    private UUID inspectionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "inspection_number", nullable = false, length = 50)
    private String inspectionNumber;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "inspection_type", nullable = false, length = 50)
    private String inspectionType;

    @Column(name = "inspection_stage", length = 50)
    private String inspectionStage;

    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate;

    @Column(name = "inspector_id")
    private UUID inspectorId;

    @Column(name = "inspector_name", length = 255)
    private String inspectorName;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "overall_result", length = 50)
    private String overallResult;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "sample_method", length = 50)
    private String sampleMethod;

    @Column(name = "quantity_inspected", precision = 15, scale = 3)
    private BigDecimal quantityInspected;

    @Column(name = "quantity_passed", precision = 15, scale = 3)
    private BigDecimal quantityPassed;

    @Column(name = "quantity_failed", precision = 15, scale = 3)
    private BigDecimal quantityFailed;

    @Column(name = "quantity_reworked", precision = 15, scale = 3)
    private BigDecimal quantityReworked;

    @Column(name = "defects_found")
    private Integer defectsFound;

    @Column(name = "critical_defects")
    private Integer criticalDefects;

    @Column(name = "major_defects")
    private Integer majorDefects;

    @Column(name = "minor_defects")
    private Integer minorDefects;

    @Column(name = "pass_rate", precision = 5, scale = 2)
    private BigDecimal passRate;

    @Column(name = "defect_rate", precision = 5, scale = 2)
    private BigDecimal defectRate;

    @Column(name = "inspection_criteria", columnDefinition = "TEXT")
    private String inspectionCriteria;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "corrective_actions", columnDefinition = "TEXT")
    private String correctiveActions;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "qualityInspection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QualityInspectionItem> inspectionItems;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (defectsFound == null) defectsFound = 0;
        if (criticalDefects == null) criticalDefects = 0;
        if (majorDefects == null) majorDefects = 0;
        if (minorDefects == null) minorDefects = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

