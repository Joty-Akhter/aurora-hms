package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "epf_accounts", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "epf_account_id")
    private UUID epfAccountId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "epf_account_number", nullable = false, length = 50)
    private String epfAccountNumber;
    
    @Column(name = "uan_number", length = 50)
    private String uanNumber;
    
    @Column(name = "account_status", length = 50)
    @Builder.Default
    private String accountStatus = "active";
    
    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;
    
    @Column(name = "closing_date")
    private LocalDate closingDate;
    
    @Column(name = "current_balance", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal currentBalance = java.math.BigDecimal.ZERO;
    
    @Column(name = "employee_contribution_balance", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employeeContributionBalance = java.math.BigDecimal.ZERO;
    
    @Column(name = "employer_contribution_balance", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerContributionBalance = java.math.BigDecimal.ZERO;
    
    @Column(name = "interest_balance", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal interestBalance = java.math.BigDecimal.ZERO;
    
    @Column(name = "last_contribution_date")
    private LocalDate lastContributionDate;
    
    @Column(name = "last_interest_calculation_date")
    private LocalDate lastInterestCalculationDate;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

