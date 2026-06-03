package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_history", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MedicationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    private UUID historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    // Historical Medication Information (snapshot)
    @Column(name = "medication_name", nullable = false, length = 500)
    private String medicationName;
    
    @Column(name = "generic_name", length = 500)
    private String genericName;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_code_type", length = 20)
    @Enumerated(EnumType.STRING)
    private Medication.MedicationCodeType medicationCodeType;
    
    @Column(name = "dosage_strength", precision = 10, scale = 3)
    private BigDecimal dosageStrength;
    
    @Column(name = "dosage_unit", length = 50)
    private String dosageUnit;
    
    @Column(name = "dosage_form", length = 50)
    @Enumerated(EnumType.STRING)
    private Medication.DosageForm dosageForm;
    
    @Column(name = "route", length = 50)
    @Enumerated(EnumType.STRING)
    private Medication.Route route;
    
    @Column(name = "frequency", length = 200)
    private String frequency;
    
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
    
    // Date Range
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    // Status Information
    @Column(name = "medication_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Medication.MedicationStatus medicationStatus;
    
    @Column(name = "status_date", nullable = false)
    private LocalDate statusDate;
    
    @Column(name = "discontinuation_reason", columnDefinition = "TEXT")
    private String discontinuationReason;
    
    // Source Information
    @Column(name = "medication_source", length = 50)
    @Enumerated(EnumType.STRING)
    private Medication.MedicationSource medicationSource;
    
    @Column(name = "prescription_id")
    private UUID prescriptionId;
    
    @Column(name = "prescribing_provider_name", length = 200)
    private String prescribingProviderName;
    
    // Indication
    @Column(name = "indication", columnDefinition = "TEXT")
    private String indication;
    
    @Column(name = "diagnosis_code", length = 20)
    private String diagnosisCode;
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
}
