package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "development_plans", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DevelopmentPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "plan_id")
    private UUID planId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;
    
    @Column(name = "plan_type", length = 50)
    private String planType;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;
    
    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;
    
    @Column(name = "status", length = 50)
    private String status = "active";
    
    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;
    
    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;
    
    @Column(name = "required_resources", columnDefinition = "TEXT")
    private String requiredResources;
    
    @Column(name = "success_criteria", columnDefinition = "TEXT")
    private String successCriteria;
    
    @Column(name = "progress_notes", columnDefinition = "TEXT")
    private String progressNotes;
    
    @Column(name = "manager_id")
    private UUID managerId;
    
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

