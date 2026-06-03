package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.LabResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultTrendResponse {
    
    private UUID patientId;
    private String testName;
    private String loincCode;
    private String testCategory;
    private String resultUnits;
    
    // Trend Data Points
    private List<TrendDataPoint> dataPoints;
    
    // Trend Statistics
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal averageValue;
    private BigDecimal medianValue;
    private Integer totalDataPoints;
    
    // Trend Direction
    private String trendDirection; // INCREASING, DECREASING, STABLE, FLUCTUATING
    private BigDecimal trendSlope; // Rate of change per day
    
    // Reference Range
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    
    // Time Range
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private UUID resultId;
        private LocalDateTime resultDate;
        private BigDecimal value;
        private String resultValue; // For non-numeric results
        private LabResult.AbnormalFlag abnormalFlag;
        private Boolean isCriticalValue;
        private String resultStatus;
    }
}
