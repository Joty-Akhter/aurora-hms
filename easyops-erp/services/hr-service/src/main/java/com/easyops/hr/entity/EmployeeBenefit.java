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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_benefits", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmployeeBenefit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "employee_benefit_id")
    private UUID employeeBenefitId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "benefit_id", nullable = false)
    private UUID benefitId;
    
    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "status", length = 50)
    private String status = "active";
    
    @Column(name = "employee_contribution", precision = 12, scale = 2)
    private BigDecimal employeeContribution;
    
    @Column(name = "employer_contribution", precision = 12, scale = 2)
    private BigDecimal employerContribution;
    
    @Column(name = "coverage_amount", precision = 12, scale = 2)
    private BigDecimal coverageAmount;
    
    @Column(name = "beneficiary_name", length = 200)
    private String beneficiaryName;
    
    @Column(name = "beneficiary_relationship", length = 50)
    private String beneficiaryRelationship;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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

