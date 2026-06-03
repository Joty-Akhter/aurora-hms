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
@Table(name = "lab_results", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LabResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "result_id")
    private UUID resultId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrder order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Result Identification
    @Column(name = "result_number", unique = true, nullable = false, length = 100)
    private String resultNumber;
    
    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;
    
    @Column(name = "loinc_code", nullable = false, length = 20)
    private String loincCode;
    
    @Column(name = "test_category", length = 100)
    private String testCategory;
    
    @Column(name = "test_type", length = 100)
    private String testType;
    
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
    
    @Column(name = "result_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ResultStatus resultStatus = ResultStatus.FINAL;
    
    // Reference Ranges
    @Column(name = "reference_range_low", precision = 20, scale = 6)
    private BigDecimal referenceRangeLow;
    
    @Column(name = "reference_range_high", precision = 20, scale = 6)
    private BigDecimal referenceRangeHigh;
    
    @Column(name = "reference_range_units", length = 50)
    private String referenceRangeUnits;
    
    @Column(name = "reference_range_text", length = 500)
    private String referenceRangeText;
    
    @Column(name = "reference_range_source", length = 100)
    private String referenceRangeSource;
    
    @Column(name = "age_specific_range")
    private Boolean ageSpecificRange = false;
    
    @Column(name = "gender_specific_range")
    private Boolean genderSpecificRange = false;
    
    // Abnormal Flags
    @Column(name = "abnormal_flag", length = 10)
    @Enumerated(EnumType.STRING)
    private AbnormalFlag abnormalFlag;
    
    @Column(name = "is_critical_value")
    private Boolean isCriticalValue = false;
    
    @Column(name = "is_delta_check")
    private Boolean isDeltaCheck = false;
    
    @Column(name = "is_panic_value")
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
    
    @Column(name = "interpretation_notes", columnDefinition = "TEXT")
    private String interpretationNotes;
    
    // Temporal Information
    @Column(name = "order_date")
    private LocalDateTime orderDate;
    
    @Column(name = "specimen_collection_date", nullable = false)
    private LocalDateTime specimenCollectionDate;
    
    @Column(name = "specimen_received_date")
    private LocalDateTime specimenReceivedDate;
    
    @Column(name = "result_date", nullable = false)
    private LocalDateTime resultDate;
    
    @Column(name = "result_reported_date", nullable = false)
    private LocalDateTime resultReportedDate;
    
    @Column(name = "result_verified_date")
    private LocalDateTime resultVerifiedDate;
    
    // Specimen Information
    @Column(name = "specimen_type", length = 100)
    private String specimenType;
    
    @Column(name = "specimen_source", length = 100)
    private String specimenSource;
    
    @Column(name = "specimen_collection_method", length = 200)
    private String specimenCollectionMethod;
    
    @Column(name = "specimen_id", length = 100)
    private String specimenId;
    
    @Column(name = "specimen_volume", length = 50)
    private String specimenVolume;
    
    @Column(name = "specimen_quality", length = 50)
    private String specimenQuality;
    
    // Laboratory Information
    @Column(name = "performing_laboratory_name", nullable = false, length = 200)
    private String performingLaboratoryName;
    
    @Column(name = "laboratory_id", length = 100)
    private String laboratoryId;
    
    @Column(name = "laboratory_npi", length = 20)
    private String laboratoryNpi;
    
    @Column(name = "laboratory_address_line1", length = 255)
    private String laboratoryAddressLine1;
    
    @Column(name = "laboratory_address_line2", length = 255)
    private String laboratoryAddressLine2;
    
    @Column(name = "laboratory_city", length = 100)
    private String laboratoryCity;
    
    @Column(name = "laboratory_state", length = 50)
    private String laboratoryState;
    
    @Column(name = "laboratory_zip", length = 20)
    private String laboratoryZip;
    
    @Column(name = "laboratory_phone", length = 50)
    private String laboratoryPhone;
    
    @Column(name = "performing_technologist", length = 200)
    private String performingTechnologist;
    
    @Column(name = "reviewing_pathologist", length = 200)
    private String reviewingPathologist;
    
    @Column(name = "reviewing_physician", length = 200)
    private String reviewingPhysician;
    
    @Column(name = "laboratory_reference_number", length = 100)
    private String laboratoryReferenceNumber;
    
    // Result Comments
    @Column(name = "laboratory_comments", columnDefinition = "TEXT")
    private String laboratoryComments;
    
    @Column(name = "provider_comments", columnDefinition = "TEXT")
    private String providerComments;
    
    @Column(name = "result_notes", columnDefinition = "TEXT")
    private String resultNotes;
    
    @Column(name = "method_used", length = 200)
    private String methodUsed;
    
    // Critical Value Management
    @Column(name = "is_critical_value_acknowledged")
    private Boolean isCriticalValueAcknowledged = false;
    
    @Column(name = "critical_value_acknowledged_by")
    private UUID criticalValueAcknowledgedBy;
    
    @Column(name = "critical_value_acknowledged_date")
    private LocalDateTime criticalValueAcknowledgedDate;
    
    @Column(name = "critical_value_response", columnDefinition = "TEXT")
    private String criticalValueResponse;
    
    // Result Review
    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;
    
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;
    
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    // Result History
    @Column(name = "is_corrected")
    private Boolean isCorrected = false;
    
    @Column(name = "is_amended")
    private Boolean isAmended = false;
    
    @Column(name = "is_cancelled")
    private Boolean isCancelled = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_result_id")
    private LabResult originalResult;
    
    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;
    
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "correction_date")
    private LocalDateTime correctionDate;
    
    @Column(name = "amendment_date")
    private LocalDateTime amendmentDate;
    
    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;
    
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
    
    public enum ResultStatus {
        FINAL, PRELIMINARY, CORRECTED, CANCELLED, AMENDED
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
}
