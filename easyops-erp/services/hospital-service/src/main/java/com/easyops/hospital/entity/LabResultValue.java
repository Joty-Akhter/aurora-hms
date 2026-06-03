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

/**
 * Entity for storing individual test result values within a test panel order.
 * When a lab order is a test panel (e.g., Complete Blood Count, Comprehensive Metabolic Panel),
 * this table stores the individual test results that make up the panel.
 */
@Entity
@Table(name = "lab_result_values", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LabResultValue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "value_id")
    private UUID valueId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private LabResult result;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrder order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Test Information
    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;
    
    @Column(name = "loinc_code", length = 20)
    private String loincCode;
    
    @Column(name = "test_category", length = 100)
    private String testCategory;
    
    @Column(name = "test_type", length = 100)
    private String testType;
    
    @Column(name = "sequence_number")
    @Builder.Default
    private Integer sequenceNumber = 1;
    
    // Result Values
    @Column(name = "result_value", length = 500)
    private String resultValue;
    
    @Column(name = "result_value_numeric", precision = 20, scale = 6)
    private BigDecimal resultValueNumeric;
    
    @Column(name = "result_units", length = 50)
    private String resultUnits;
    
    @Column(name = "result_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ResultType resultType;
    
    @Column(name = "qualitative_result", length = 100)
    private String qualitativeResult;
    
    @Column(name = "quantitative_result", precision = 20, scale = 6)
    private BigDecimal quantitativeResult;
    
    // Reference Ranges
    @Column(name = "reference_range_low", precision = 20, scale = 6)
    private BigDecimal referenceRangeLow;
    
    @Column(name = "reference_range_high", precision = 20, scale = 6)
    private BigDecimal referenceRangeHigh;
    
    @Column(name = "reference_range_units", length = 50)
    private String referenceRangeUnits;
    
    @Column(name = "reference_range_text", length = 500)
    private String referenceRangeText;
    
    // Abnormal Flags
    @Column(name = "abnormal_flag", length = 10)
    @Enumerated(EnumType.STRING)
    private AbnormalFlag abnormalFlag;
    
    @Column(name = "is_critical_value")
    @Builder.Default
    private Boolean isCriticalValue = false;
    
    @Column(name = "is_panic_value")
    @Builder.Default
    private Boolean isPanicValue = false;
    
    @Column(name = "result_interpretation", length = 100)
    private String resultInterpretation;
    
    // Clinical Significance
    @Column(name = "clinical_significance", length = 50)
    @Enumerated(EnumType.STRING)
    private ClinicalSignificance clinicalSignificance;
    
    @Column(name = "clinical_significance_level", length = 20)
    @Enumerated(EnumType.STRING)
    private ClinicalSignificanceLevel clinicalSignificanceLevel;
    
    // Laboratory Information
    @Column(name = "performing_laboratory_name", length = 200)
    private String performingLaboratoryName;
    
    @Column(name = "laboratory_comments", columnDefinition = "TEXT")
    private String laboratoryComments;
    
    @Column(name = "method_used", length = 200)
    private String methodUsed;
    
    // Result Status
    @Column(name = "result_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResultStatus resultStatus = ResultStatus.FINAL;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum ResultType {
        NUMERIC, TEXT, CODED, STRUCTURED
    }
    
    public enum AbnormalFlag {
        H, L, A, N, C
    }
    
    public enum ClinicalSignificance {
        NORMAL, ABNORMAL, CRITICAL, SIGNIFICANT_CHANGE, TRENDING, STABLE
    }
    
    public enum ClinicalSignificanceLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum ResultStatus {
        FINAL, PRELIMINARY, CORRECTED, AMENDED, CANCELLED
    }
}
