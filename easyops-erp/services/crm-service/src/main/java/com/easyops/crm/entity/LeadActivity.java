package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_activities", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LeadActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "activity_id")
    private UUID activityId;
    
    @Column(name = "lead_id", nullable = false)
    private UUID leadId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType; // CALL, EMAIL, MEETING, TASK, NOTE
    
    @Column(name = "subject", length = 255)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "activity_date")
    private LocalDateTime activityDate;
    
    // Status
    @Column(name = "status", length = 50)
    private String status = "PLANNED"; // PLANNED, COMPLETED, CANCELLED
    
    @Column(name = "outcome", length = 50)
    private String outcome; // INTERESTED, NOT_INTERESTED, CALLBACK, NO_ANSWER, MEETING_SCHEDULED
    
    // Task fields
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "priority", length = 20)
    private String priority; // HIGH, MEDIUM, LOW
    
    // Communication fields
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "next_action", length = 255)
    private String nextAction;
    
    @Column(name = "next_action_date")
    private LocalDateTime nextActionDate;
    
    // Audit
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


