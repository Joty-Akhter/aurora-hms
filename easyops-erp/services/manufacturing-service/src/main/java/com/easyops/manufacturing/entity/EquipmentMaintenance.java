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
@Table(name = "equipment_maintenance", schema = "manufacturing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentMaintenance {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "maintenance_id", updatable = false, nullable = false)
    private UUID maintenanceId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "maintenance_number", nullable = false, length = 50)
    private String maintenanceNumber;

    @Column(name = "work_center_id", nullable = false)
    private UUID workCenterId;

    @Column(name = "maintenance_type", nullable = false, length = 50)
    private String maintenanceType;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "scheduled_duration_hours", precision = 5, scale = 2)
    private BigDecimal scheduledDurationHours;

    @Column(name = "actual_start_date")
    private LocalDateTime actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDateTime actualEndDate;

    @Column(name = "actual_duration_hours", precision = 5, scale = 2)
    private BigDecimal actualDurationHours;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "work_performed", columnDefinition = "TEXT")
    private String workPerformed;

    @Column(name = "parts_replaced", columnDefinition = "TEXT")
    private String partsReplaced;

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(name = "technician_name", length = 255)
    private String technicianName;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "parts_cost", precision = 10, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "downtime_hours", precision = 10, scale = 2)
    private BigDecimal downtimeHours;

    @Column(name = "production_loss", precision = 15, scale = 2)
    private BigDecimal productionLoss;

    @Column(name = "follow_up_required")
    private Boolean followUpRequired;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

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
        if (followUpRequired == null) followUpRequired = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

