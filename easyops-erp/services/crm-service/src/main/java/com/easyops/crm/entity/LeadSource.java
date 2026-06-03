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
@Table(name = "lead_sources", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LeadSource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "source_id")
    private UUID sourceId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;
    
    @Column(name = "source_code", nullable = false, length = 50)
    private String sourceCode;
    
    @Column(name = "source_type", length = 50)
    private String sourceType; // WEB, REFERRAL, CAMPAIGN, IMPORT, API, MANUAL, EVENT, PARTNER
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
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


