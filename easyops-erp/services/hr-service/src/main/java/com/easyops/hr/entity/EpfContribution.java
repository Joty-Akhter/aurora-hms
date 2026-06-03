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
@Table(name = "epf_contributions", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfContribution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "contribution_id")
    private UUID contributionId;
    
    @Column(name = "epf_account_id", nullable = false)
    private UUID epfAccountId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "contribution_period_start", nullable = false)
    private LocalDate contributionPeriodStart;
    
    @Column(name = "contribution_period_end", nullable = false)
    private LocalDate contributionPeriodEnd;
    
    @Column(name = "contribution_month", nullable = false)
    private Integer contributionMonth;
    
    @Column(name = "contribution_year", nullable = false)
    private Integer contributionYear;
    
    @Column(name = "employee_basic_salary", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal employeeBasicSalary;

    /** INT-09: PF wage base after ceiling (may differ from basic salary). */
    @Column(name = "pf_wage_base", precision = 14, scale = 2)
    private java.math.BigDecimal pfWageBase;
    
    @Column(name = "employee_contribution_rate", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employeeContributionRate = new java.math.BigDecimal("12.00");
    
    @Column(name = "employee_contribution_amount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal employeeContributionAmount;
    
    @Column(name = "employer_contribution_rate", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerContributionRate = new java.math.BigDecimal("12.00");
    
    @Column(name = "employer_contribution_amount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal employerContributionAmount;

    @Column(name = "employer_epf_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerEpfAmount = java.math.BigDecimal.ZERO;

    @Column(name = "employer_pension_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerPensionAmount = java.math.BigDecimal.ZERO;

    @Column(name = "employer_edli_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerEdliAmount = java.math.BigDecimal.ZERO;

    @Column(name = "employer_admin_charge_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal employerAdminChargeAmount = java.math.BigDecimal.ZERO;

    @Column(name = "total_contribution", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal totalContribution;
    
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private java.math.BigDecimal interestRate;
    
    @Column(name = "interest_amount", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal interestAmount = java.math.BigDecimal.ZERO;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "pending";
    
    @Column(name = "processed_date")
    private LocalDate processedDate;
    
    @Column(name = "payroll_run_id")
    private UUID payrollRunId;
    
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

