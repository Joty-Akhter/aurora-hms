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
@Table(name = "campaigns", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "campaign_id")
    private UUID campaignId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "campaign_number", nullable = false, length = 50)
    private String campaignNumber;
    
    @Column(name = "campaign_name", nullable = false, length = 255)
    private String campaignName;
    
    @Column(name = "campaign_type", length = 50)
    private String campaignType;
    
    @Column(name = "status", length = 50)
    private String status = "PLANNING";
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "budgeted_cost", precision = 15, scale = 2)
    private BigDecimal budgetedCost;
    
    @Column(name = "actual_cost", precision = 15, scale = 2)
    private BigDecimal actualCost;
    
    @Column(name = "currency", length = 10)
    private String currency = "USD";
    
    @Column(name = "expected_response_count")
    private Integer expectedResponseCount;
    
    @Column(name = "expected_revenue", precision = 15, scale = 2)
    private BigDecimal expectedRevenue;
    
    @Column(name = "owner_id")
    private UUID ownerId;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;
    
    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;
    
    @Column(name = "utm_source", length = 255)
    private String utmSource;
    
    @Column(name = "utm_medium", length = 255)
    private String utmMedium;
    
    @Column(name = "utm_campaign", length = 255)
    private String utmCampaign;
    
    @Column(name = "utm_term", length = 255)
    private String utmTerm;
    
    @Column(name = "utm_content", length = 255)
    private String utmContent;
    
    @Column(name = "email_template_id")
    private UUID emailTemplateId;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

