package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsSummaryResponse {
    
    private VitalSignsResponse latest;
    private List<VitalSignsResponse> recent;
    private List<VitalSignsTrendResponse> trends;
    private Long totalMeasurements;
    private Long abnormalCount;
    private Long criticalCount;
}
