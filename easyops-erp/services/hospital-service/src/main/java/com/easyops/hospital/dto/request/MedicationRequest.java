package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Medication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    @NotBlank(message = "Medication name is required")
    private String medicationName;
    
    private String genericName;
    private String medicationCode;
    private Medication.MedicationCodeType medicationCodeType;
    private String ndcCode;
    private String rxnormCode;
    
    // Dosage Information
    private BigDecimal dosageStrength;
    private String dosageUnit;
    private Medication.DosageForm dosageForm;
    private BigDecimal quantity;
    private String quantityUnit;
    
    // Administration Instructions
    private Medication.Route route;
    private String frequency;
    private String timing;
    private String instructions;
    
    // Prescription Information
    private UUID prescriptionId;
    private UUID prescribingProviderId;
    private String prescribingProviderName;
    private String prescribingProviderNpi;
    private LocalDate prescriptionDate;
    private UUID pharmacyId;
    private String pharmacyName;
    private Integer refillsAuthorized;
    private Integer refillsRemaining;
    
    // Medication Status
    private Medication.MedicationStatus medicationStatus;
    private LocalDate statusDate;
    
    // Indication/Reason
    private String indication;
    private String diagnosisCode;
    
    // Medication Source
    @NotNull(message = "Medication source is required")
    private Medication.MedicationSource medicationSource;
    
    // Date Information
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    private LocalDate lastFilledDate;
    
    // Additional Information
    private String notes;
    private String specialInstructions;
}
