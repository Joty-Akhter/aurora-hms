package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "sla_policies", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlaPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sla_id")
    private UUID slaId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;
    
    @Column(name = "policy_code", nullable = false, length = 100)
    private String policyCode;
    
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;
    
    @Column(name = "first_response_time", nullable = false)
    private Integer firstResponseTime;
    
    @Column(name = "resolution_time", nullable = false)
    private Integer resolutionTime;
    
    @Column(name = "business_hours_only")
    private Boolean businessHoursOnly = true;
    
    @Column(name = "business_hours_start")
    private LocalTime businessHoursStart;
    
    @Column(name = "business_hours_end")
    private LocalTime businessHoursEnd;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
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

