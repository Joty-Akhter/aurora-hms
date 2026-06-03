package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Medication;
import com.easyops.hospital.entity.MedicationReconciliationComparison;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReconciliationComparisonResponse {
    
    private UUID comparisonId;
    private UUID reconciliationId;
    private String medicationName;
    private String genericName;
    private String medicationCode;
    private Medication.MedicationCodeType medicationCodeType;
    private MedicationReconciliationComparison.ComparisonStatus comparisonStatus;
    private MedicationReconciliationComparison.ActionTaken actionTaken;
    private UUID sourceMedicationId;
    private UUID targetMedicationId;
    private BigDecimal beforeDosageStrength;
    private BigDecimal afterDosageStrength;
    private String beforeDosageUnit;
    private String afterDosageUnit;
    private String beforeFrequency;
    private String afterFrequency;
    private String beforeRoute;
    private String afterRoute;
    private String beforeInstructions;
    private String afterInstructions;
    private String differences;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private UUID createdBy;
}
