package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunity_activities", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "activity_id")
    private UUID activityId;
    
    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;
    
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "activity_date")
    private LocalDateTime activityDate;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "priority", length = 20)
    private String priority;
    
    @Column(name = "assigned_to")
    private UUID assignedTo;
    
    @Column(name = "completed_by")
    private UUID completedBy;
    
    @Column(name = "outcome", length = 50)
    private String outcome;
    
    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;
    
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
        if (activityDate == null) {
            activityDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

