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
import java.util.UUID;

@Entity
@Table(name = "medications", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Medication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "medication_id")
    private UUID medicationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    // Medication Identification
    @Column(name = "medication_name", nullable = false, length = 500)
    private String medicationName;
    
    @Column(name = "generic_name", length = 500)
    private String genericName;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_code_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MedicationCodeType medicationCodeType;
    
    @Column(name = "ndc_code", length = 20)
    private String ndcCode;
    
    @Column(name = "rxnorm_code", length = 20)
    private String rxnormCode;
    
    // Dosage Information
    @Column(name = "dosage_strength", precision = 10, scale = 3)
    private BigDecimal dosageStrength;
    
    @Column(name = "dosage_unit", length = 50)
    private String dosageUnit;
    
    @Column(name = "dosage_form", length = 50)
    @Enumerated(EnumType.STRING)
    private DosageForm dosageForm;
    
    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;
    
    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit;
    
    // Administration Instructions
    @Column(name = "route", length = 50)
    @Enumerated(EnumType.STRING)
    private Route route;
    
    @Column(name = "frequency", length = 200)
    private String frequency;
    
    @Column(name = "timing", length = 200)
    private String timing;
    
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
    
    // Prescription Information
    @Column(name = "prescription_id")
    private UUID prescriptionId;
    
    @Column(name = "prescribing_provider_id")
    private UUID prescribingProviderId;
    
    @Column(name = "prescribing_provider_name", length = 200)
    private String prescribingProviderName;
    
    @Column(name = "prescribing_provider_npi", length = 20)
    private String prescribingProviderNpi;
    
    @Column(name = "prescription_date")
    private LocalDate prescriptionDate;
    
    @Column(name = "pharmacy_id")
    private UUID pharmacyId;
    
    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;
    
    @Column(name = "refills_authorized")
    @Builder.Default
    private Integer refillsAuthorized = 0;
    
    @Column(name = "refills_remaining")
    @Builder.Default
    private Integer refillsRemaining = 0;
    
    // Medication Status
    @Column(name = "medication_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MedicationStatus medicationStatus = MedicationStatus.ACTIVE;
    
    @Column(name = "status_date")
    private LocalDate statusDate;
    
    @Column(name = "status_changed_by")
    private UUID statusChangedBy;
    
    // Indication/Reason
    @Column(name = "indication", columnDefinition = "TEXT")
    private String indication;
    
    @Column(name = "diagnosis_code", length = 20)
    private String diagnosisCode;
    
    // Medication Source
    @Column(name = "medication_source", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MedicationSource medicationSource;
    
    // Date Information
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "last_filled_date")
    private LocalDate lastFilledDate;
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
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
    
    public enum MedicationCodeType {
        RXNORM, NDC, OTHER
    }
    
    public enum DosageForm {
        TABLET, CAPSULE, LIQUID, INJECTION, TOPICAL, INHALATION,
        SUBLINGUAL, BUCCAL, RECTAL, OPHTHALMIC, OTIC, NASAL, OTHER
    }
    
    public enum Route {
        ORAL, IV, IM, SC, TOPICAL, INHALATION, SUBLINGUAL,
        BUCCAL, RECTAL, OPHTHALMIC, OTIC, NASAL, OTHER
    }
    
    public enum MedicationStatus {
        ACTIVE, DISCONTINUED, ON_HOLD, COMPLETED
    }
    
    public enum MedicationSource {
        PRESCRIPTION, PATIENT_REPORTED, PHARMACY, CLINICAL_DOCUMENTATION, EXTERNAL_IMPORT, OTHER
    }
}
