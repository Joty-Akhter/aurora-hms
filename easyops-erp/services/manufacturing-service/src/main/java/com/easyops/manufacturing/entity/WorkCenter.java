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
@Table(name = "work_centers", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkCenter {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "work_center_id", updatable = false, nullable = false)
    private UUID workCenterId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "work_center_code", nullable = false, length = 50)
    private String workCenterCode;

    @Column(name = "work_center_name", nullable = false, length = 255)
    private String workCenterName;

    @Column(name = "work_center_type", length = 50)
    private String workCenterType;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "capacity_per_hour", precision = 10, scale = 2)
    private BigDecimal capacityPerHour;

    @Column(name = "capacity_uom", length = 20)
    private String capacityUom;

    @Column(name = "number_of_machines")
    private Integer numberOfMachines;

    @Column(name = "number_of_operators")
    private Integer numberOfOperators;

    @Column(name = "shifts_per_day")
    private Integer shiftsPerDay;

    @Column(name = "hours_per_shift", precision = 5, scale = 2)
    private BigDecimal hoursPerShift;

    @Column(name = "working_days_per_week")
    private Integer workingDaysPerWeek;

    @Column(name = "efficiency_percentage", precision = 5, scale = 2)
    private BigDecimal efficiencyPercentage;

    @Column(name = "utilization_percentage", precision = 5, scale = 2)
    private BigDecimal utilizationPercentage;

    @Column(name = "oee_target", precision = 5, scale = 2)
    private BigDecimal oeeTarget;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "cost_per_hour", precision = 10, scale = 2)
    private BigDecimal costPerHour;

    @Column(name = "setup_cost", precision = 10, scale = 2)
    private BigDecimal setupCost;

    @Column(name = "overhead_rate", precision = 5, scale = 2)
    private BigDecimal overheadRate;

    @Column(name = "last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDateTime nextMaintenanceDate;

    @Column(name = "maintenance_frequency_days")
    private Integer maintenanceFrequencyDays;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

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
        if (isActive == null) isActive = true;
        if (numberOfMachines == null) numberOfMachines = 1;
        if (shiftsPerDay == null) shiftsPerDay = 1;
        if (hoursPerShift == null) hoursPerShift = BigDecimal.valueOf(8);
        if (workingDaysPerWeek == null) workingDaysPerWeek = 5;
        if (efficiencyPercentage == null) efficiencyPercentage = BigDecimal.valueOf(100);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

