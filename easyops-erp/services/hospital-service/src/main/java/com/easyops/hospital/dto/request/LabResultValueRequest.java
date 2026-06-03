package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.LabResultValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultValueRequest {
    
    private UUID resultId;
    private UUID orderId;
    private UUID patientId;
    
    // Test Information
    private String testName;
    private String loincCode;
    private String testCategory;
    private String testType;
    private Integer sequenceNumber;
    
    // Result Values
    private String resultValue;
    private BigDecimal resultValueNumeric;
    private String resultUnits;
    private LabResultValue.ResultType resultType;
    private String qualitativeResult;
    private BigDecimal quantitativeResult;
    
    // Reference Ranges
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeUnits;
    private String referenceRangeText;
    
    // Abnormal Flags
    private LabResultValue.AbnormalFlag abnormalFlag;
    private Boolean isCriticalValue;
    private Boolean isPanicValue;
    private String resultInterpretation;
    
    // Clinical Significance
    private LabResultValue.ClinicalSignificance clinicalSignificance;
    private LabResultValue.ClinicalSignificanceLevel clinicalSignificanceLevel;
    
    // Laboratory Information
    private String performingLaboratoryName;
    private String laboratoryComments;
    private String methodUsed;
    
    // Result Status
    private LabResultValue.ResultStatus resultStatus;
}
