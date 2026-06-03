package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Prescription;
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
public class PrescriptionMedicationRequest {

    /** When updating, optional client-side id for correlation (ignored by server if not UUID) */
    private UUID prescriptionMedicationId;

    @NotBlank(message = "Medication name is required")
    private String medicationName;

    private String medicationCode;
    private Prescription.MedicationCodeType medicationCodeType;

    private BigDecimal dosageStrength;
    private String dosageUnit;

    @NotNull(message = "Dosage form is required")
    private Prescription.DosageForm dosageForm;

    @NotNull(message = "Route is required")
    private Prescription.Route route;

    private String frequency;
    private String instructions;

    @NotNull(message = "Start date is required")
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
