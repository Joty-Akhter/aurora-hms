package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_members", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "member_id")
    private UUID memberId;
    
    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "lead_id")
    private UUID leadId;
    
    @Column(name = "contact_id")
    private UUID contactId;
    
    @Column(name = "account_id")
    private UUID accountId;
    
    @Column(name = "member_status", length = 50)
    private String memberStatus = "SENT";
    
    @Column(name = "responded")
    private Boolean responded = false;
    
    @Column(name = "response_date")
    private LocalDateTime responseDate;
    
    @Column(name = "converted")
    private Boolean converted = false;
    
    @Column(name = "conversion_date")
    private LocalDateTime conversionDate;
    
    @Column(name = "email_sent")
    private Boolean emailSent = false;
    
    @Column(name = "email_opened")
    private Boolean emailOpened = false;
    
    @Column(name = "email_clicked")
    private Boolean emailClicked = false;
    
    @Column(name = "opened_at")
    private LocalDateTime openedAt;
    
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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

