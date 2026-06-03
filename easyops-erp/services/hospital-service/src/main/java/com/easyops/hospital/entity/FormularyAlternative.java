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
@Table(name = "formulary_alternatives", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FormularyAlternative {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alternative_id")
    private UUID alternativeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formulary_check_id", nullable = false)
    private FormularyCheck formularyCheck;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_name", length = 500)
    private String medicationName;
    
    @Column(name = "generic_name", length = 500)
    private String genericName;
    
    @Column(name = "formulary_tier", length = 20)
    private String formularyTier;
    
    @Column(name = "coverage_status", length = 50)
    @Enumerated(EnumType.STRING)
    private CoverageStatus coverageStatus;
    
    @Column(name = "requires_prior_authorization")
    @Builder.Default
    private Boolean requiresPriorAuthorization = false;
    
    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;
    
    @Column(name = "patient_cost_estimate", precision = 10, scale = 2)
    private BigDecimal patientCostEstimate;
    
    @Column(name = "alternative_type", length = 50)
    @Enumerated(EnumType.STRING)
    private AlternativeType alternativeType;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Why this is an alternative
    
    @Column(name = "is_preferred")
    @Builder.Default
    private Boolean isPreferred = false;
    
    @Column(name = "rank")
    private Integer rank; // Ranking of alternative (1 = best)
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CoverageStatus {
        COVERED, NOT_COVERED, COVERED_WITH_RESTRICTIONS
    }
    
    public enum AlternativeType {
        GENERIC, THERAPEUTIC, FORMULARY, PREFERRED
    }
}
