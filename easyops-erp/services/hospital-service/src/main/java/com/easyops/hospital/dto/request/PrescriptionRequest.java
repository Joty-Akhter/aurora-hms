package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.EpEncounterMode;
import com.easyops.hospital.entity.Prescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;

    /** EP-1 / EP-11: OPD vs IPD session when the prescription was authored. */
    private EpEncounterMode epEncounterMode;

    private String prescriptionNumber;
    
    private Prescription.PrescriptionType prescriptionType;

    /**
     * Preferred: one or more medicines. When omitted, legacy single-medication fields below are used.
     */
    @Valid
    private List<PrescriptionMedicationRequest> medications;

    /** Legacy single medication (used when {@code medications} is null or empty) */
    private String medicationName;

    private String medicationCode;
    private Prescription.MedicationCodeType medicationCodeType;

    private BigDecimal dosageStrength;
    private String dosageUnit;

    private Prescription.DosageForm dosageForm;

    private Prescription.Route route;

    private String frequency;
    private String instructions;

    private LocalDate startDate;

    private LocalDate endDate;
    private Integer durationDays;

    private Integer refillsAuthorized;
    private Integer refillsRemaining;

    private Boolean substitutionAllowed;
    private String dawCode;

    private Boolean isControlledSubstance;
    private Prescription.Schedule schedule;
    private String deaNumber;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    private String pharmacyAddressLine1;
    private String pharmacyAddressLine2;
    private String pharmacyCity;
    private String pharmacyState;
    private String pharmacyZip;
    private String pharmacyPhone;
    
    @NotNull(message = "Prescribing provider ID is required")
    private UUID prescribingProviderId;
    
    private String prescribingProviderNpi;
    private String prescribingProviderName;
    
    private String notes;
    private String specialInstructions;

    /**
     * Legacy single-code field; used when {@code diagnoses} is null or empty.
     * Prefer {@code diagnoses} for new callers (FR-P1.4a).
     */
    private String diagnosisCode;

    /**
     * FR-P1.4a: one or more ICD-10 diagnoses.
     * When provided, takes precedence over {@code diagnosisCode}.
     * The first entry with {@code isPrimary = true} (or index 0 if none marked) becomes the primary diagnosis.
     */
    @Valid
    private List<PrescriptionDiagnosisRequest> diagnoses;
}
