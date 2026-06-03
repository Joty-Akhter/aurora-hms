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
@Table(name = "quality_inspection_items", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityInspectionItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspection qualityInspection;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "parameter_code", length = 50)
    private String parameterCode;

    @Column(name = "parameter_name", nullable = false, length = 255)
    private String parameterName;

    @Column(name = "parameter_type", length = 50)
    private String parameterType;

    @Column(name = "specification_min", precision = 15, scale = 4)
    private BigDecimal specificationMin;

    @Column(name = "specification_max", precision = 15, scale = 4)
    private BigDecimal specificationMax;

    @Column(name = "specification_target", precision = 15, scale = 4)
    private BigDecimal specificationTarget;

    @Column(name = "specification_unit", length = 20)
    private String specificationUnit;

    @Column(name = "specification_text", columnDefinition = "TEXT")
    private String specificationText;

    @Column(name = "measured_value", precision = 15, scale = 4)
    private BigDecimal measuredValue;

    @Column(name = "measured_text", length = 255)
    private String measuredText;

    @Column(name = "pass_fail", length = 10)
    private String passFail;

    @Column(name = "is_critical")
    private Boolean isCritical;

    @Column(name = "deviation", precision = 15, scale = 4)
    private BigDecimal deviation;

    @Column(name = "deviation_percentage", precision = 5, scale = 2)
    private BigDecimal deviationPercentage;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isCritical == null) isCritical = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

