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
public class LabResultListViewResponse {
    
    private UUID resultId;
    private UUID orderId;
    private String orderNumber;
    private UUID patientId;
    private String patientName;
    private String mrn;
    
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
    private LabResult.ResultStatus resultStatus;
    
    // Reference Ranges
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeUnits;
    
    // Abnormal Flags
    private LabResult.AbnormalFlag abnormalFlag;
    private Boolean isCriticalValue;
    private Boolean isDeltaCheck;
    private Boolean isPanicValue;
    
    // Highlighting Information
    private String highlightColor; // RED, ORANGE, YELLOW, GREEN, NONE
    private String highlightReason; // CRITICAL, ABNORMAL, DELTA_CHECK, etc.
    private Boolean requiresAttention;
    
    // Temporal Information
    private LocalDateTime resultDate;
    private LocalDateTime resultReportedDate;
    
    // Quick Status Indicators
    private Boolean isReviewed;
    private Boolean isCriticalValueAcknowledged;
    
    // Laboratory Information
    private String performingLaboratoryName;
}
