package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Medication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationHistoryResponse {
    
    private UUID historyId;
    private UUID medicationId;
    private UUID patientId;
    
    // Historical Medication Information
    private String medicationName;
    private String genericName;
    private String medicationCode;
    private Medication.MedicationCodeType medicationCodeType;
    private BigDecimal dosageStrength;
    private String dosageUnit;
    private Medication.DosageForm dosageForm;
    private Medication.Route route;
    private String frequency;
    private String instructions;
    
    // Date Range
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Status Information
    private Medication.MedicationStatus medicationStatus;
    private LocalDate statusDate;
    private String discontinuationReason;
    
    // Source Information
    private Medication.MedicationSource medicationSource;
    private UUID prescriptionId;
    private String prescribingProviderName;
    
    // Indication
    private String indication;
    private String diagnosisCode;
    
    // Additional Information
    private String notes;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private UUID createdBy;
}
