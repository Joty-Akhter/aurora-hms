package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Goal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "goal_id")
    private UUID goalId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "cycle_id")
    private UUID cycleId;
    
    @Column(name = "goal_title", nullable = false, length = 200)
    private String goalTitle;
    
    @Column(name = "goal_description", columnDefinition = "TEXT")
    private String goalDescription;
    
    @Column(name = "goal_category", length = 100)
    private String goalCategory;
    
    @Column(name = "target_value", precision = 10, scale = 2)
    private BigDecimal targetValue;
    
    @Column(name = "current_value", precision = 10, scale = 2)
    private BigDecimal currentValue = BigDecimal.ZERO;
    
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;
    
    @Column(name = "priority", length = 50)
    private String priority = "medium";
    
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ZERO;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;
    
    @Column(name = "completion_date")
    private LocalDate completionDate;
    
    @Column(name = "status", length = 50)
    private String status = "in_progress";
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;
    
    @Column(name = "manager_id")
    private UUID managerId;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

