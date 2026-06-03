package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.VitalSigns;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsRequest {
    
    @NotNull(message = "Measurement date is required")
    private LocalDate measurementDate;
    
    @NotNull(message = "Measurement time is required")
    private LocalTime measurementTime;
    
    private UUID encounterId;
    
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
}
