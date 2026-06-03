package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Prescription;
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
public class PrescriptionMedicationResponse {

    private UUID prescriptionMedicationId;
    private Integer lineNumber;

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
}
