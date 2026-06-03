package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "formulary_checks", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FormularyCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "formulary_check_id")
    private UUID formularyCheckId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @Column(name = "insurance_id")
    private UUID insuranceId;
    
    @Column(name = "insurance_company_name", length = 200)
    private String insuranceCompanyName;
    
    @Column(name = "policy_number", length = 100)
    private String policyNumber;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_name", length = 500)
    private String medicationName;
    
    @Column(name = "coverage_status", length = 50)
    @Enumerated(EnumType.STRING)
    private CoverageStatus coverageStatus;
    
    @Column(name = "formulary_tier", length = 20)
    private String formularyTier; // Tier 1, Tier 2, Tier 3, Non-Formulary
    
    @Column(name = "requires_prior_authorization")
    @Builder.Default
    private Boolean requiresPriorAuthorization = false;
    
    @Column(name = "prior_authorization_required")
    @Builder.Default
    private Boolean priorAuthorizationRequired = false;
    
    @Column(name = "step_therapy_required")
    @Builder.Default
    private Boolean stepTherapyRequired = false;
    
    @Column(name = "quantity_limit")
    private Integer quantityLimit;
    
    @Column(name = "days_supply_limit")
    private Integer daysSupplyLimit;
    
    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;
    
    @Column(name = "coinsurance_percentage", precision = 5, scale = 2)
    private BigDecimal coinsurancePercentage;
    
    @Column(name = "deductible_applies")
    @Builder.Default
    private Boolean deductibleApplies = false;
    
    @Column(name = "patient_cost_estimate", precision = 10, scale = 2)
    private BigDecimal patientCostEstimate;
    
    @Column(name = "insurance_pays", precision = 10, scale = 2)
    private BigDecimal insurancePays;
    
    @Column(name = "pbm_name", length = 200)
    private String pbmName; // Pharmacy Benefit Manager name
    
    @Column(name = "pbm_id", length = 100)
    private String pbmId;
    
    @Column(name = "formulary_id", length = 100)
    private String formularyId;
    
    @Column(name = "formulary_name", length = 200)
    private String formularyName;
    
    @Column(name = "check_date")
    @Builder.Default
    private LocalDateTime checkDate = LocalDateTime.now();
    
    @Column(name = "check_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CheckStatus checkStatus = CheckStatus.PENDING;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData; // JSON response from PBM
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CoverageStatus {
        COVERED, NOT_COVERED, COVERED_WITH_RESTRICTIONS, UNKNOWN, ERROR
    }
    
    public enum CheckStatus {
        PENDING, COMPLETED, FAILED, TIMEOUT
    }
}
