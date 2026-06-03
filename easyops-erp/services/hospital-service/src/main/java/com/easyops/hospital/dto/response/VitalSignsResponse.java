package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.VitalSigns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsResponse {
    
    private UUID vitalSignId;
    private UUID patientId;
    private UUID encounterId;
    private LocalDate measurementDate;
    private LocalTime measurementTime;
    
    // Blood Pressure
    private Integer systolicBp;
    private Integer diastolicBp;
    
    // Heart Rate and Respiratory
    private Integer heartRate;
    private Integer respiratoryRate;
    
    // Temperature
    private BigDecimal temperature;
    private VitalSigns.TemperatureUnit temperatureUnit;
    
    // Oxygen Saturation
    private BigDecimal oxygenSaturation;
    
    // Weight and Height
    private BigDecimal weight;
    private VitalSigns.WeightUnit weightUnit;
    private BigDecimal height;
    private VitalSigns.HeightUnit heightUnit;
    
    // BMI (calculated)
    private BigDecimal bmi;
    
    // Additional Measurements
    private Integer painScale;
    private BigDecimal bloodGlucose;
    private BigDecimal headCircumference;
    
    // Context Information
    private UUID measuredBy;
    private UUID measurementLocationId;
    private String deviceUsed;
    private String patientPosition;
    private String notes;
    
    // Status Flags
    private Boolean isAbnormal;
    private Boolean isCritical;
    private String abnormalReason;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
