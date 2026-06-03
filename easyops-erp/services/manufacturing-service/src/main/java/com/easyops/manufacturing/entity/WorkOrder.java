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
@Table(name = "work_orders", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "work_order_id", updatable = false, nullable = false)
    private UUID workOrderId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "work_order_number", nullable = false, length = 50)
    private String workOrderNumber;

    // Product & BOM Reference
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "bom_id")
    private UUID bomId;

    @Column(name = "routing_id")
    private UUID routingId;

    // Order Type & Source
    @Column(name = "order_type", length = 50)
    private String orderType; // PRODUCTION, REWORK, ASSEMBLY, DISASSEMBLY

    @Column(name = "source_type", length = 50)
    private String sourceType; // SALES_ORDER, STOCK_REPLENISHMENT, MRP, MANUAL

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    // Quantities
    @Column(name = "quantity_planned", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityPlanned;

    @Column(name = "quantity_completed", precision = 15, scale = 3)
    private BigDecimal quantityCompleted;

    @Column(name = "quantity_scrapped", precision = 15, scale = 3)
    private BigDecimal quantityScrapped;

    @Column(name = "quantity_reworked", precision = 15, scale = 3)
    private BigDecimal quantityReworked;

    @Column(name = "quantity_in_process", precision = 15, scale = 3)
    private BigDecimal quantityInProcess;

    // Status & Priority
    @Column(name = "status", length = 50)
    private String status; // CREATED, RELEASED, IN_PROGRESS, PAUSED, COMPLETED, CLOSED, CANCELLED

    @Column(name = "priority", length = 20)
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    // Dates
    @Column(name = "planned_start_date")
    private LocalDateTime plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDateTime plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDateTime actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDateTime actualEndDate;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    // Location & Warehouse
    @Column(name = "source_warehouse_id")
    private UUID sourceWarehouseId;

    @Column(name = "target_warehouse_id")
    private UUID targetWarehouseId;

    @Column(name = "production_line", length = 100)
    private String productionLine;

    @Column(name = "work_center_id")
    private UUID workCenterId;

    // Costs
    @Column(name = "material_cost", precision = 18, scale = 2)
    private BigDecimal materialCost;

    @Column(name = "labor_cost", precision = 18, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "overhead_cost", precision = 18, scale = 2)
    private BigDecimal overheadCost;

    @Column(name = "total_cost", precision = 18, scale = 2)
    private BigDecimal totalCost;

    // Progress Tracking
    @Column(name = "completion_percentage", precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "operations_completed")
    private Integer operationsCompleted;

    @Column(name = "total_operations")
    private Integer totalOperations;

    // Additional Info
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    // Audit
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkOrderOperation> operations;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkOrderMaterial> materials;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantityCompleted == null) quantityCompleted = BigDecimal.ZERO;
        if (quantityScrapped == null) quantityScrapped = BigDecimal.ZERO;
        if (quantityReworked == null) quantityReworked = BigDecimal.ZERO;
        if (quantityInProcess == null) quantityInProcess = BigDecimal.ZERO;
        if (materialCost == null) materialCost = BigDecimal.ZERO;
        if (laborCost == null) laborCost = BigDecimal.ZERO;
        if (overheadCost == null) overheadCost = BigDecimal.ZERO;
        if (totalCost == null) totalCost = BigDecimal.ZERO;
        if (completionPercentage == null) completionPercentage = BigDecimal.ZERO;
        if (operationsCompleted == null) operationsCompleted = 0;
        if (totalOperations == null) totalOperations = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

