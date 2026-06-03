package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "task_id")
    private UUID taskId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "task_type", length = 50)
    private String taskType;
    
    @Column(name = "status", length = 50)
    private String status = "NOT_STARTED";
    
    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM";
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "reminder_date")
    private LocalDateTime reminderDate;
    
    @Column(name = "completed_date")
    private LocalDate completedDate;
    
    @Column(name = "assigned_to")
    private UUID assignedTo;
    
    @Column(name = "assigned_by")
    private UUID assignedBy;
    
    @Column(name = "lead_id")
    private UUID leadId;
    
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "opportunity_id")
    private UUID opportunityId;
    
    @Column(name = "campaign_id")
    private UUID campaignId;
    
    @Column(name = "completed_by")
    private UUID completedBy;
    
    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;
    
    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern;
    
    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;
    
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

