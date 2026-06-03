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
@Table(name = "work_order_operations", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderOperation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "operation_id", updatable = false, nullable = false)
    private UUID operationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    // Routing Reference
    @Column(name = "routing_operation_id")
    private UUID routingOperationId;

    @Column(name = "operation_sequence", nullable = false)
    private Integer operationSequence;

    @Column(name = "operation_code", length = 50)
    private String operationCode;

    @Column(name = "operation_name", nullable = false, length = 255)
    private String operationName;

    // Work Center & Assignment
    @Column(name = "work_center_id")
    private UUID workCenterId;

    @Column(name = "work_center_code", length = 50)
    private String workCenterCode;

    @Column(name = "work_center_name", length = 255)
    private String workCenterName;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    // Status & Progress
    @Column(name = "status", length = 50)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, PAUSED, SKIPPED, FAILED

    // Time Standards
    @Column(name = "setup_time_planned", precision = 10, scale = 2)
    private BigDecimal setupTimePlanned;

    @Column(name = "run_time_planned", precision = 10, scale = 2)
    private BigDecimal runTimePlanned;

    @Column(name = "teardown_time_planned", precision = 10, scale = 2)
    private BigDecimal teardownTimePlanned;

    @Column(name = "total_time_planned", precision = 10, scale = 2)
    private BigDecimal totalTimePlanned;

    // Actual Time Tracking
    @Column(name = "setup_time_actual", precision = 10, scale = 2)
    private BigDecimal setupTimeActual;

    @Column(name = "run_time_actual", precision = 10, scale = 2)
    private BigDecimal runTimeActual;

    @Column(name = "teardown_time_actual", precision = 10, scale = 2)
    private BigDecimal teardownTimeActual;

    @Column(name = "total_time_actual", precision = 10, scale = 2)
    private BigDecimal totalTimeActual;

    // Dates & Times
    @Column(name = "planned_start")
    private LocalDateTime plannedStart;

    @Column(name = "planned_end")
    private LocalDateTime plannedEnd;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    // Production Tracking
    @Column(name = "quantity_completed", precision = 15, scale = 3)
    private BigDecimal quantityCompleted;

    @Column(name = "quantity_rejected", precision = 15, scale = 3)
    private BigDecimal quantityRejected;

    @Column(name = "quantity_reworked", precision = 15, scale = 3)
    private BigDecimal quantityReworked;

    // Quality Check
    @Column(name = "quality_check_required")
    private Boolean qualityCheckRequired;

    @Column(name = "quality_check_completed")
    private Boolean qualityCheckCompleted;

    @Column(name = "quality_inspector")
    private UUID qualityInspector;

    @Column(name = "quality_check_date")
    private LocalDateTime qualityCheckDate;

    @Column(name = "quality_result", length = 50)
    private String qualityResult; // PASS, FAIL, CONDITIONAL_PASS

    // Cost
    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "overhead_cost", precision = 10, scale = 2)
    private BigDecimal overheadCost;

    // Instructions & Notes
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Metadata
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantityCompleted == null) quantityCompleted = BigDecimal.ZERO;
        if (quantityRejected == null) quantityRejected = BigDecimal.ZERO;
        if (quantityReworked == null) quantityReworked = BigDecimal.ZERO;
        if (qualityCheckRequired == null) qualityCheckRequired = false;
        if (qualityCheckCompleted == null) qualityCheckCompleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

