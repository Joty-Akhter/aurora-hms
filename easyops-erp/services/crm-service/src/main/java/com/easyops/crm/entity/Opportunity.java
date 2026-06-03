package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunities", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Opportunity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "opportunity_id")
    private UUID opportunityId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "opportunity_number", nullable = false, length = 50)
    private String opportunityNumber;
    
    @Column(name = "opportunity_name", nullable = false, length = 255)
    private String opportunityName;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "lead_id")
    private UUID leadId;
    
    @Column(name = "stage_id")
    private UUID stageId;
    
    @Column(name = "type", length = 50)
    private String type;
    
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 10)
    private String currency = "USD";
    
    @Column(name = "probability", precision = 5, scale = 2)
    private BigDecimal probability;
    
    @Column(name = "expected_revenue", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal expectedRevenue;
    
    @Column(name = "close_date")
    private LocalDate closeDate;
    
    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;
    
    @Column(name = "created_date")
    private LocalDate createdDate;
    
    @Column(name = "owner_id")
    private UUID ownerId;
    
    @Column(name = "status", length = 50)
    private String status = "OPEN";
    
    @Column(name = "lead_source_id")
    private UUID leadSourceId;
    
    @Column(name = "source_campaign", length = 255)
    private String sourceCampaign;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "next_step", columnDefinition = "TEXT")
    private String nextStep;
    
    @Column(name = "competitors", columnDefinition = "TEXT")
    private String competitors;
    
    @Column(name = "loss_reason", length = 255)
    private String lossReason;
    
    @Column(name = "loss_description", columnDefinition = "TEXT")
    private String lossDescription;
    
    @Column(name = "win_description", columnDefinition = "TEXT")
    private String winDescription;
    
    @Column(name = "days_in_stage")
    private Integer daysInStage = 0;
    
    @Column(name = "total_days_open")
    private Integer totalDaysOpen;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "priority", length = 20)
    private String priority;
    
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
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

