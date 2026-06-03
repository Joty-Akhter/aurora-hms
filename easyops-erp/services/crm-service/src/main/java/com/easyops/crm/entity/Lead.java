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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Lead {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lead_id")
    private UUID leadId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "lead_number", nullable = false, length = 50)
    private String leadNumber;
    
    // Basic Info
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "mobile", length = 50)
    private String mobile;
    
    @Column(name = "company", length = 255)
    private String company;
    
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    
    // Lead Details
    @Column(name = "lead_source_id")
    private UUID leadSourceId;
    
    @Column(name = "source_campaign", length = 255)
    private String sourceCampaign;
    
    @Column(name = "status", length = 50)
    private String status = "NEW"; // NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED, LOST
    
    @Column(name = "rating", length = 20)
    private String rating; // HOT, WARM, COLD
    
    @Column(name = "lead_score")
    private Integer leadScore = 0;
    
    // Address
    @Column(name = "street", length = 255)
    private String street;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 100)
    private String country;
    
    // Business Info
    @Column(name = "industry", length = 100)
    private String industry;
    
    @Column(name = "company_size", length = 50)
    private String companySize;
    
    @Column(name = "annual_revenue", precision = 15, scale = 2)
    private BigDecimal annualRevenue;
    
    @Column(name = "website", length = 255)
    private String website;
    
    // Assignment
    @Column(name = "owner_id")
    private UUID ownerId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    // Qualification
    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;
    
    @Column(name = "qualified_by")
    private UUID qualifiedBy;
    
    @Column(name = "disqualified_reason", columnDefinition = "TEXT")
    private String disqualifiedReason;
    
    // Conversion
    @Column(name = "converted_at")
    private LocalDateTime convertedAt;
    
    @Column(name = "converted_by")
    private UUID convertedBy;
    
    @Column(name = "converted_account_id")
    private UUID convertedAccountId;
    
    @Column(name = "converted_contact_id")
    private UUID convertedContactId;
    
    @Column(name = "converted_opportunity_id")
    private UUID convertedOpportunityId;
    
    // Marketing Attribution
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
    
    // Additional
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "consent_email")
    private Boolean consentEmail = false;
    
    @Column(name = "consent_phone")
    private Boolean consentPhone = false;
    
    @Column(name = "do_not_contact")
    private Boolean doNotContact = false;
    
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


