package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsTrendResponse {
    
    private LocalDate measurementDate;
    private BigDecimal avgSystolicBp;
    private BigDecimal avgDiastolicBp;
    private BigDecimal avgHeartRate;
    private BigDecimal avgRespiratoryRate;
    private BigDecimal avgTemperature;
    private BigDecimal avgOxygenSaturation;
    private BigDecimal avgWeight;
    private BigDecimal avgBmi;
    private Long measurementCount;
}
