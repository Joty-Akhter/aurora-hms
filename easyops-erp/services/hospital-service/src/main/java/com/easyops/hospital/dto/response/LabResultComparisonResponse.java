package com.easyops.hospital.dto.response;

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
public class LabResultComparisonResponse {
    
    private UUID currentResultId;
    private UUID previousResultId;
    
    private String testName;
    private String loincCode;
    private String testCategory;
    
    // Current Result
    private LabResultResponse currentResult;
    
    // Previous Result
    private LabResultResponse previousResult;
    
    // Comparison Metrics
    private BigDecimal absoluteDifference;
    private BigDecimal percentChange;
    private String changeDirection; // INCREASED, DECREASED, UNCHANGED
    private Boolean isSignificantChange; // True if change exceeds threshold
    private String comparisonNotes;
    
    // Time between results
    private Long daysBetweenResults;
    
    private LocalDateTime comparisonDate;
}
