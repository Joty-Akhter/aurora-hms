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
import java.util.UUID;

@Entity
@Table(name = "product_routings", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRouting {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "routing_id", updatable = false, nullable = false)
    private UUID routingId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "routing_number", nullable = false, length = 50)
    private String routingNumber;

    // Product Reference
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "bom_id")
    private UUID bomId;

    // Routing Details
    @Column(name = "routing_name", length = 255)
    private String routingName;

    @Column(name = "operation_sequence", nullable = false)
    private Integer operationSequence;

    @Column(name = "operation_code", length = 50)
    private String operationCode;

    @Column(name = "operation_name", nullable = false, length = 255)
    private String operationName;

    // Work Center
    @Column(name = "work_center_code", length = 50)
    private String workCenterCode;

    @Column(name = "work_center_name", length = 255)
    private String workCenterName;

    // Time Standards (in minutes)
    @Column(name = "setup_time", precision = 10, scale = 2)
    private BigDecimal setupTime;

    @Column(name = "run_time_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal runTimePerUnit;

    @Column(name = "teardown_time", precision = 10, scale = 2)
    private BigDecimal teardownTime;

    // Costs
    @Column(name = "cost_per_hour", precision = 10, scale = 2)
    private BigDecimal costPerHour;

    @Column(name = "setup_cost", precision = 10, scale = 2)
    private BigDecimal setupCost;

    // Operation Details
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "quality_check_required")
    private Boolean qualityCheckRequired;

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

