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
@Table(name = "bom_headers", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomHeader {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "bom_id", updatable = false, nullable = false)
    private UUID bomId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "bom_number", nullable = false, length = 50)
    private String bomNumber;

    // Product Reference
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    // BOM Details
    @Column(name = "bom_type", length = 50)
    private String bomType; // MANUFACTURING, ENGINEERING, SALES, SERVICE, PHANTOM

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "revision", length = 20)
    private String revision;

    // Status
    @Column(name = "status", length = 50)
    private String status; // DRAFT, APPROVED, ACTIVE, OBSOLETE

    // Effective Dates
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    // Quantities
    @Column(name = "base_quantity", precision = 15, scale = 3)
    private BigDecimal baseQuantity;

    @Column(name = "uom", length = 20)
    private String uom;

    // Costs
    @Column(name = "material_cost", precision = 15, scale = 2)
    private BigDecimal materialCost;

    @Column(name = "labor_cost", precision = 15, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "overhead_cost", precision = 15, scale = 2)
    private BigDecimal overheadCost;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    // Additional Info
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Approval
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    // Metadata
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "bomHeader", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BomLine> bomLines;

    @OneToMany(mappedBy = "bomHeader", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BomVersion> bomVersions;

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

