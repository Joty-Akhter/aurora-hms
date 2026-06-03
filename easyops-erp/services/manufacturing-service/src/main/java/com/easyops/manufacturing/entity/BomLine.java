package com.easyops.manufacturing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bom_lines", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomLine {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "bom_line_id", updatable = false, nullable = false)
    private UUID bomLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomHeader bomHeader;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    // Component Details
    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "component_code", length = 100)
    private String componentCode;

    @Column(name = "component_name", length = 255)
    private String componentName;

    // Hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_line_id")
    private BomLine parentLine;

    @OneToMany(mappedBy = "parentLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BomLine> childLines;

    @Column(name = "level_number")
    private Integer levelNumber;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    // Quantity Requirements
    @Column(name = "quantity_per_unit", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityPerUnit;

    @Column(name = "uom", length = 20)
    private String uom;

    // Component Classification
    @Column(name = "component_type", length = 50)
    private String componentType; // RAW_MATERIAL, SUB_ASSEMBLY, FINISHED_GOOD, PACKAGING

    // Characteristics
    @Column(name = "is_optional")
    private Boolean isOptional;

    @Column(name = "is_phantom")
    private Boolean isPhantom;

    @Column(name = "can_substitute")
    private Boolean canSubstitute;

    // Planning
    @Column(name = "scrap_percentage", precision = 5, scale = 2)
    private BigDecimal scrapPercentage;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    // Costs
    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    // Additional Info
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reference_designator", length = 100)
    private String referenceDesignator;

    // Status
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    // Metadata
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

