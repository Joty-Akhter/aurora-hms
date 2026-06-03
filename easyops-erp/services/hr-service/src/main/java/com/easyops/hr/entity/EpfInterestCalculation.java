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
@Table(name = "epf_interest_calculations", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfInterestCalculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "interest_calculation_id")
    private UUID interestCalculationId;
    
    @Column(name = "epf_account_id", nullable = false)
    private UUID epfAccountId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "financial_year", nullable = false)
    private Integer financialYear;
    
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal interestRate;
    
    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal openingBalance;
    
    @Column(name = "total_contributions", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal totalContributions;
    
    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal interestAmount;
    
    @Column(name = "closing_balance", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal closingBalance;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "calculated"; // calculated, posted, reversed
    
    @Column(name = "posted_date")
    private LocalDate postedDate;
    
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

