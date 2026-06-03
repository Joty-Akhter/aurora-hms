package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "vital_signs", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VitalSigns {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "vital_sign_id")
    private UUID vitalSignId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;
    
    @Column(name = "measurement_time", nullable = false)
    private LocalTime measurementTime;
    
    // Blood Pressure
    @Column(name = "systolic_bp")
    private Integer systolicBp;
    
    @Column(name = "diastolic_bp")
    private Integer diastolicBp;
    
    // Heart Rate and Respiratory
    @Column(name = "heart_rate")
    private Integer heartRate;
    
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;
    
    // Temperature
    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;
    
    @Column(name = "temperature_unit", length = 1)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TemperatureUnit temperatureUnit = TemperatureUnit.F;
    
    // Oxygen Saturation
    @Column(name = "oxygen_saturation", precision = 5, scale = 2)
    private BigDecimal oxygenSaturation;
    
    // Weight and Height
    @Column(name = "weight", precision = 6, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "weight_unit", length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WeightUnit weightUnit = WeightUnit.lbs;
    
    @Column(name = "height", precision = 6, scale = 2)
    private BigDecimal height;
    
    @Column(name = "height_unit", length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HeightUnit heightUnit = HeightUnit.in;
    
    // BMI (calculated)
    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;
    
    // Additional Measurements
    @Column(name = "pain_scale")
    private Integer painScale;
    
    @Column(name = "blood_glucose", precision = 6, scale = 2)
    private BigDecimal bloodGlucose;
    
    @Column(name = "head_circumference", precision = 6, scale = 2)
    private BigDecimal headCircumference;
    
    // Context Information
    @Column(name = "measured_by")
    private UUID measuredBy;
    
    @Column(name = "measurement_location_id")
    private UUID measurementLocationId;
    
    @Column(name = "device_used", length = 200)
    private String deviceUsed;
    
    @Column(name = "patient_position", length = 50)
    private String patientPosition;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Status Flags
    @Column(name = "is_abnormal")
    @Builder.Default
    private Boolean isAbnormal = false;
    
    @Column(name = "is_critical")
    @Builder.Default
    private Boolean isCritical = false;
    
    @Column(name = "abnormal_reason", columnDefinition = "TEXT")
    private String abnormalReason;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum TemperatureUnit {
        C, F
    }
    
    public enum WeightUnit {
        lbs, kg
    }
    
    public enum HeightUnit {
        in, cm, m
    }
}
