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
@Table(name = "work_order_materials", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderMaterial {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "material_id", updatable = false, nullable = false)
    private UUID materialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    // Material Reference
    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "component_code", length = 100)
    private String componentCode;

    @Column(name = "component_name", length = 255)
    private String componentName;

    // BOM Reference
    @Column(name = "bom_line_id")
    private UUID bomLineId;

    // Operation Assignment
    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "operation_sequence")
    private Integer operationSequence;

    // Quantities
    @Column(name = "quantity_required", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityRequired;

    @Column(name = "quantity_reserved", precision = 15, scale = 4)
    private BigDecimal quantityReserved;

    @Column(name = "quantity_issued", precision = 15, scale = 4)
    private BigDecimal quantityIssued;

    @Column(name = "quantity_consumed", precision = 15, scale = 4)
    private BigDecimal quantityConsumed;

    @Column(name = "quantity_returned", precision = 15, scale = 4)
    private BigDecimal quantityReturned;

    @Column(name = "quantity_scrapped", precision = 15, scale = 4)
    private BigDecimal quantityScrapped;

    // Unit of Measure
    @Column(name = "uom", length = 20)
    private String uom;

    // Status
    @Column(name = "status", length = 50)
    private String status; // PLANNED, RESERVED, ISSUED, CONSUMED, RETURNED

    // Location
    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    // Costs
    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    // Dates
    @Column(name = "required_date")
    private LocalDateTime requiredDate;

    @Column(name = "reserved_date")
    private LocalDateTime reservedDate;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "consumed_date")
    private LocalDateTime consumedDate;

    // Backflush
    @Column(name = "backflush")
    private Boolean backflush;

    // Additional Info
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantityReserved == null) quantityReserved = BigDecimal.ZERO;
        if (quantityIssued == null) quantityIssued = BigDecimal.ZERO;
        if (quantityConsumed == null) quantityConsumed = BigDecimal.ZERO;
        if (quantityReturned == null) quantityReturned = BigDecimal.ZERO;
        if (quantityScrapped == null) quantityScrapped = BigDecimal.ZERO;
        if (backflush == null) backflush = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

