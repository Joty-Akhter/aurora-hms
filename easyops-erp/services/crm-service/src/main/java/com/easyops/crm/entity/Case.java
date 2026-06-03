package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cases", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Case {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "case_id")
    private UUID caseId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "case_number", nullable = false, length = 50)
    private String caseNumber;
    
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "case_type", length = 50)
    private String caseType;
    
    @Column(name = "status", length = 50)
    private String status = "NEW";
    
    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM";
    
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "assigned_to")
    private UUID assignedTo;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "origin", length = 50)
    private String origin;
    
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "sla_id")
    private UUID slaId;
    
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;
    
    @Column(name = "first_response_due")
    private LocalDateTime firstResponseDue;
    
    @Column(name = "resolution_due")
    private LocalDateTime resolutionDue;
    
    @Column(name = "sla_breached")
    private Boolean slaBreached = false;
    
    @Column(name = "customer_rating")
    private Integer customerRating;
    
    @Column(name = "customer_feedback", columnDefinition = "TEXT")
    private String customerFeedback;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "subcategory", length = 100)
    private String subcategory;
    
    @Column(name = "parent_case_id")
    private UUID parentCaseId;
    
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

