package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_checklists", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OnboardingChecklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "checklist_id")
    private UUID checklistId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;
    
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;
    
    @Column(name = "task_category", length = 100)
    private String taskCategory;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "completed_date")
    private LocalDate completedDate;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";
    
    @Column(name = "assigned_to")
    private UUID assignedTo;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

