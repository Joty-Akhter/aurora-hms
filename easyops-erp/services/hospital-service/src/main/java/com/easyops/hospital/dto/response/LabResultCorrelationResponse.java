package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultCorrelationResponse {
    
    private UUID patientId;
    private LocalDateTime collectionDate;
    private UUID encounterId;
    
    // Primary Result
    private LabResultResponse primaryResult;
    
    // Related Results (same collection date/time, same order, or related tests)
    private List<LabResultResponse> relatedResults;
    
    // Correlation Group Information
    private String correlationGroup; // e.g., "CBC", "LIPID_PANEL", "LIVER_FUNCTION"
    private String correlationReason; // Why these results are grouped together
    
    // Summary
    private Integer totalRelatedResults;
    private Integer abnormalResultsCount;
    private Integer criticalResultsCount;
}
