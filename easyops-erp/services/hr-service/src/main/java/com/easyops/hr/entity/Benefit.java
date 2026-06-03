package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "benefits", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "benefit_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Benefit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "benefit_id")
    private UUID benefitId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "benefit_name", nullable = false, length = 200)
    private String benefitName;
    
    @Column(name = "benefit_type", nullable = false, length = 50)
    private String benefitType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "provider_name", length = 200)
    private String providerName;
    
    @Column(name = "coverage_type", length = 50)
    private String coverageType;
    
    @Column(name = "employer_contribution", precision = 12, scale = 2)
    private BigDecimal employerContribution;
    
    @Column(name = "employee_contribution", precision = 12, scale = 2)
    private BigDecimal employeeContribution;
    
    @Column(name = "is_mandatory")
    private Boolean isMandatory = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

