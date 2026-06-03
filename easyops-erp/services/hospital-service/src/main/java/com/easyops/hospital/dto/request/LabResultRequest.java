package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.LabResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class LabResultRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private UUID patientId;
    private UUID encounterId;
    
    private String resultNumber;
    
    @NotBlank(message = "Test name is required")
    private String testName;
    
    @NotBlank(message = "LOINC code is required")
    private String loincCode;
    
    private String testCategory;
    private String testType;
    
    // Result Values
    private String resultValue;
    private BigDecimal resultValueNumeric;
    private String resultUnits;
    
    @NotNull(message = "Result type is required")
    private LabResult.ResultType resultType;
    
    private String qualitativeResult;
    private BigDecimal quantitativeResult;
    
    private LabResult.ResultStatus resultStatus = LabResult.ResultStatus.FINAL;
    
    // Reference Ranges
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeUnits;
    private String referenceRangeText;
    private String referenceRangeSource;
    private Boolean ageSpecificRange = false;
    private Boolean genderSpecificRange = false;
    
    // Abnormal Flags
    private LabResult.AbnormalFlag abnormalFlag;
    private Boolean isCriticalValue = false;
    private Boolean isDeltaCheck = false;
    private Boolean isPanicValue = false;
    private String resultInterpretation;
    
    // Clinical Significance
    private LabResult.ClinicalSignificance clinicalSignificance;
    private LabResult.ClinicalSignificanceLevel clinicalSignificanceLevel;
    private String interpretationNotes;
    
    // Temporal Information
    private LocalDateTime orderDate;
    
    @NotNull(message = "Specimen collection date is required")
    private LocalDateTime specimenCollectionDate;
    
    private LocalDateTime specimenReceivedDate;
    
    @NotNull(message = "Result date is required")
    private LocalDateTime resultDate;
    
    @NotNull(message = "Result reported date is required")
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
    @NotBlank(message = "Performing laboratory name is required")
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
}
