package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.EpEncounterMode;
import com.easyops.hospital.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    
    private UUID prescriptionId;
    private UUID patientId;
    private UUID encounterId;

    private EpEncounterMode epEncounterMode;

    private String prescriptionNumber;
    private Prescription.PrescriptionType prescriptionType;

    /** All medicines on this prescription (line items). */
    private List<PrescriptionMedicationResponse> medications;

    /** Denormalized: first line or combined names for list views / legacy clients */
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
    private Boolean pdmpQueried;
    private LocalDateTime pdmpQueryDate;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    private String pharmacyAddressLine1;
    private String pharmacyAddressLine2;
    private String pharmacyCity;
    private String pharmacyState;
    private String pharmacyZip;
    private String pharmacyPhone;
    
    private UUID prescribingProviderId;
    private String prescribingProviderNpi;
    private String prescribingProviderName;
    
    private Prescription.PrescriptionStatus prescriptionStatus;
    
    private LocalDateTime createdDate;
    private LocalDateTime sentDate;
    private LocalDateTime filledDate;
    private LocalDateTime cancellationDate;
    private LocalDate expirationDate;
    
    private String cancellationReason;
    private UUID cancelledBy;
    
    private String notes;
    private String specialInstructions;

    /** Legacy: primary diagnosis code; kept for backward-compatible clients. */
    private String diagnosisCode;

    /** FR-P1.4a: all diagnoses associated with this prescription, ordered by sequenceOrder. */
    private List<PrescriptionDiagnosisResponse> diagnoses;

    private Boolean hasInteractions;
    private Boolean hasAllergyWarnings;
    private Prescription.ValidationStatus validationStatus;
    private String validationNotes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    // Related Data
    private List<PrescriptionInteractionResponse> interactions;
    private List<PrescriptionAllergyCheckResponse> allergyChecks;
    private Integer interactionCount;
    private Integer allergyCheckCount;
    
    // PDMP Query Results
    private PDMPQueryResponse latestPdmpQueryResult; // Most recent PDMP query result
    private List<PDMPQueryResponse> pdmpQueryResults; // All PDMP query results
    private Integer pdmpQueryCount; // Number of PDMP queries performed
    
    // Formulary Information
    private Boolean formularyChecked;
    private LocalDateTime formularyCheckDate;
    private Prescription.FormularyCoverageStatus coverageStatus;
    private String formularyTier;
    private Boolean requiresPriorAuthorization;
    private Boolean priorAuthorizationObtained;
    private String priorAuthorizationNumber;
    private BigDecimal patientCostEstimate;
    private BigDecimal copayAmount;
    private UUID insuranceId;
    private String pbmName;
}
