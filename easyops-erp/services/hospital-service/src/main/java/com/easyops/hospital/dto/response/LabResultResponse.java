package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.LabResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {
    
    private UUID resultId;
    private UUID orderId;
    private String orderNumber;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private UUID encounterId;
    private UUID organizationId;
    
    private String resultNumber;
    private String testName;
    private String loincCode;
    private String testCategory;
    private String testType;
    
    // Result Values
    private String resultValue;
    private BigDecimal resultValueNumeric;
    private String resultUnits;
    private LabResult.ResultType resultType;
    private String qualitativeResult;
    private BigDecimal quantitativeResult;
    private LabResult.ResultStatus resultStatus;
    
    // Reference Ranges
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeUnits;
    private String referenceRangeText;
    private String referenceRangeSource;
    private Boolean ageSpecificRange;
    private Boolean genderSpecificRange;
    
    // Abnormal Flags
    private LabResult.AbnormalFlag abnormalFlag;
    private Boolean isCriticalValue;
    private Boolean isDeltaCheck;
    private Boolean isPanicValue;
    private String resultInterpretation;
    
    // Clinical Significance
    private LabResult.ClinicalSignificance clinicalSignificance;
    private LabResult.ClinicalSignificanceLevel clinicalSignificanceLevel;
    private String interpretationNotes;
    
    // Temporal Information
    private LocalDateTime orderDate;
    private LocalDateTime specimenCollectionDate;
    private LocalDateTime specimenReceivedDate;
    private LocalDateTime resultDate;
    private LocalDateTime resultReportedDate;
    private LocalDateTime resultVerifiedDate;
    
    // Specimen Information
    private String specimenType;
    private String specimenSource;
    private String specimenCollectionMethod;
    private String specimenId;
    private String specimenVolume;
    private String specimenQuality;
    
    // Laboratory Information
    private String performingLaboratoryName;
    private String laboratoryId;
    private String laboratoryNpi;
    private String laboratoryAddressLine1;
    private String laboratoryAddressLine2;
    private String laboratoryCity;
    private String laboratoryState;
    private String laboratoryZip;
    private String laboratoryPhone;
    private String performingTechnologist;
    private String reviewingPathologist;
    private String reviewingPhysician;
    private String laboratoryReferenceNumber;
    
    // Result Comments
    private String laboratoryComments;
    private String providerComments;
    private String resultNotes;
    private String methodUsed;
    
    // Critical Value Management
    private Boolean isCriticalValueAcknowledged;
    private UUID criticalValueAcknowledgedBy;
    private LocalDateTime criticalValueAcknowledgedDate;
    private String criticalValueResponse;
    
    // Result Review
    private Boolean isReviewed;
    private UUID reviewedBy;
    private LocalDateTime reviewedDate;
    private String reviewNotes;
    
    // Result History
    private Boolean isCorrected;
    private Boolean isAmended;
    private Boolean isCancelled;
    private UUID originalResultId;
    private String correctionReason;
    private String amendmentReason;
    private String cancellationReason;
    private LocalDateTime correctionDate;
    private LocalDateTime amendmentDate;
    private LocalDateTime cancellationDate;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
