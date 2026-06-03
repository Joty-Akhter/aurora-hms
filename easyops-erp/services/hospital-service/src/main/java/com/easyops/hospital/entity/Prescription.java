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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Prescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "prescription_id")
    private UUID prescriptionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;

    /** EP-1 / EP-11: outpatient vs inpatient prescribing session (optional audit dimension). */
    @Column(name = "ep_encounter_mode", length = 8)
    @Enumerated(EnumType.STRING)
    private EpEncounterMode epEncounterMode;
    
    // Prescription Identification
    @Column(name = "prescription_number", unique = true, nullable = false, length = 100)
    private String prescriptionNumber;
    
    @Column(name = "prescription_type", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PrescriptionType prescriptionType = PrescriptionType.ELECTRONIC;
    
    // Medication Information
    @Column(name = "medication_name", nullable = false, length = 500)
    private String medicationName;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_code_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MedicationCodeType medicationCodeType;
    
    // Dosage Information
    @Column(name = "dosage_strength", precision = 10, scale = 3)
    private BigDecimal dosageStrength;
    
    @Column(name = "dosage_unit", length = 50)
    private String dosageUnit;
    
    @Column(name = "dosage_form", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DosageForm dosageForm;
    
    @Column(name = "route", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Route route;
    
    @Column(name = "frequency", length = 200)
    private String frequency;
    
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
    
    // Duration
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "duration_days")
    private Integer durationDays;
    
    // Refills
    @Column(name = "refills_authorized")
    @Builder.Default
    private Integer refillsAuthorized = 0;
    
    @Column(name = "refills_remaining")
    @Builder.Default
    private Integer refillsRemaining = 0;
    
    // Substitution
    @Column(name = "substitution_allowed")
    @Builder.Default
    private Boolean substitutionAllowed = true;
    
    @Column(name = "daw_code", length = 10)
    private String dawCode;
    
    // Controlled Substance Information
    @Column(name = "is_controlled_substance")
    @Builder.Default
    private Boolean isControlledSubstance = false;
    
    @Column(name = "schedule", length = 10)
    @Enumerated(EnumType.STRING)
    private Schedule schedule;
    
    @Column(name = "dea_number", length = 20)
    private String deaNumber;
    
    @Column(name = "pdmp_queried")
    @Builder.Default
    private Boolean pdmpQueried = false;
    
    @Column(name = "pdmp_query_date")
    private LocalDateTime pdmpQueryDate;
    
    // Pharmacy Information
    @Column(name = "pharmacy_id")
    private UUID pharmacyId;
    
    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;
    
    @Column(name = "pharmacy_npi", length = 20)
    private String pharmacyNpi;
    
    @Column(name = "pharmacy_address_line1", length = 255)
    private String pharmacyAddressLine1;
    
    @Column(name = "pharmacy_address_line2", length = 255)
    private String pharmacyAddressLine2;
    
    @Column(name = "pharmacy_city", length = 100)
    private String pharmacyCity;
    
    @Column(name = "pharmacy_state", length = 50)
    private String pharmacyState;
    
    @Column(name = "pharmacy_zip", length = 20)
    private String pharmacyZip;
    
    @Column(name = "pharmacy_phone", length = 50)
    private String pharmacyPhone;
    
    // Prescribing Provider Information
    @Column(name = "prescribing_provider_id", nullable = false)
    private UUID prescribingProviderId;
    
    @Column(name = "prescribing_provider_npi", length = 20)
    private String prescribingProviderNpi;
    
    @Column(name = "prescribing_provider_name", length = 200)
    private String prescribingProviderName;
    
    // Prescription Status
    @Column(name = "prescription_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PrescriptionStatus prescriptionStatus = PrescriptionStatus.DRAFT;
    
    // Dates
    @Column(name = "created_date")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @Column(name = "sent_date")
    private LocalDateTime sentDate;
    
    @Column(name = "filled_date")
    private LocalDateTime filledDate;
    
    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    // Cancellation
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "cancelled_by")
    private UUID cancelledBy;
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;
    
    /** Legacy single-code column; kept for backward-compatibility and filled from the primary diagnosis. */
    @Column(name = "diagnosis_code", length = 20)
    private String diagnosisCode;

    /** FR-P1.4a: ordered list of ICD-10 diagnoses; exactly one should have isPrimary = true. */
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<PrescriptionDiagnosis> diagnoses = new java.util.ArrayList<>();

    // Validation Flags
    @Column(name = "has_interactions")
    @Builder.Default
    private Boolean hasInteractions = false;
    
    @Column(name = "has_allergy_warnings")
    @Builder.Default
    private Boolean hasAllergyWarnings = false;
    
    @Column(name = "validation_status", length = 50)
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;
    
    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;
    
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
    
    // Formulary Information
    @Column(name = "formulary_checked")
    @Builder.Default
    private Boolean formularyChecked = false;
    
    @Column(name = "formulary_check_date")
    private LocalDateTime formularyCheckDate;
    
    @Column(name = "coverage_status", length = 50)
    @Enumerated(EnumType.STRING)
    private FormularyCoverageStatus coverageStatus;
    
    @Column(name = "formulary_tier", length = 20)
    private String formularyTier;
    
    @Column(name = "requires_prior_authorization")
    @Builder.Default
    private Boolean requiresPriorAuthorization = false;
    
    @Column(name = "prior_authorization_obtained")
    @Builder.Default
    private Boolean priorAuthorizationObtained = false;
    
    @Column(name = "prior_authorization_number", length = 100)
    private String priorAuthorizationNumber;
    
    @Column(name = "patient_cost_estimate", precision = 10, scale = 2)
    private BigDecimal patientCostEstimate;
    
    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;
    
    @Column(name = "insurance_id")
    private UUID insuranceId; // Insurance used for formulary check
    
    @Column(name = "pbm_name", length = 200)
    private String pbmName;
    
    // Line items (medicines); denormalized columns below mirror line 1 / summary for legacy queries
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<PrescriptionMedication> medications;

    // Relationships
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionInteraction> interactions;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionAllergyCheck> allergyChecks;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionHistory> history;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormularyCheck> formularyChecks;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriorAuthorization> priorAuthorizations;
    
    public enum PrescriptionType {
        ELECTRONIC, PAPER, PHONE, FAX, OTHER
    }
    
    public enum MedicationCodeType {
        RXNORM, NDC, OTHER
    }
    
    public enum DosageForm {
        TABLET, CAPSULE, SYRUP, LIQUID, SOLUTION, SUSPENSION,
        INJECTION, INFUSION,
        CREAM, OINTMENT, LOTION, GEL,
        POWDER, GRANULES,
        INHALER, INHALATION,
        DROPS, SUPPOSITORY, SPRAY, PATCH, MOUTHWASH,
        TOPICAL, SUBLINGUAL, BUCCAL, RECTAL, OPHTHALMIC, OTIC, NASAL,
        OTHER
    }
    
    public enum Route {
        ORAL, IV, IM, SC, TOPICAL, INHALATION, SUBLINGUAL, 
        BUCCAL, RECTAL, OPHTHALMIC, OTIC, NASAL, OTHER
    }
    
    public enum Schedule {
        II, III, IV, V
    }
    
    public enum PrescriptionStatus {
        DRAFT, PENDING, SENT, FILLED, PARTIALLY_FILLED, CANCELLED, EXPIRED, REJECTED, ON_HOLD
    }
    
    public enum ValidationStatus {
        VALID, WARNINGS, ERRORS
    }
    
    public enum FormularyCoverageStatus {
        COVERED, NOT_COVERED, COVERED_WITH_RESTRICTIONS, NOT_CHECKED, ERROR
    }
}
