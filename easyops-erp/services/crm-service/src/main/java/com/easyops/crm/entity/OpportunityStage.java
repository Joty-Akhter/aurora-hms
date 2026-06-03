package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunity_stages", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityStage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "stage_id")
    private UUID stageId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;
    
    @Column(name = "stage_code", nullable = false, length = 50)
    private String stageCode;
    
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;
    
    @Column(name = "probability", precision = 5, scale = 2)
    private BigDecimal probability;
    
    @Column(name = "is_closed_won")
    private Boolean isClosedWon = false;
    
    @Column(name = "is_closed_lost")
    private Boolean isClosedLost = false;
    
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

