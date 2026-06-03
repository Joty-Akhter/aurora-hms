package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One line item (medicine) on a prescription. A prescription may contain many.
 */
@Entity
@Table(name = "prescription_medications", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "prescription_medication_id")
    private UUID prescriptionMedicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "medication_name", nullable = false, length = 500)
    private String medicationName;

    @Column(name = "medication_code", length = 100)
    private String medicationCode;

    @Column(name = "medication_code_type", length = 20)
    @Enumerated(EnumType.STRING)
    private Prescription.MedicationCodeType medicationCodeType;

    @Column(name = "dosage_strength", precision = 10, scale = 3)
    private BigDecimal dosageStrength;

    @Column(name = "dosage_unit", length = 50)
    private String dosageUnit;

    @Column(name = "dosage_form", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Prescription.DosageForm dosageForm;

    @Column(name = "route", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Prescription.Route route;

    @Column(name = "frequency", length = 200)
    private String frequency;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "refills_authorized")
    @Builder.Default
    private Integer refillsAuthorized = 0;

    @Column(name = "refills_remaining")
    @Builder.Default
    private Integer refillsRemaining = 0;

    @Column(name = "substitution_allowed")
    @Builder.Default
    private Boolean substitutionAllowed = true;

    @Column(name = "daw_code", length = 10)
    private String dawCode;

    @Column(name = "is_controlled_substance")
    @Builder.Default
    private Boolean isControlledSubstance = false;

    @Column(name = "schedule", length = 10)
    @Enumerated(EnumType.STRING)
    private Prescription.Schedule schedule;

    @Column(name = "dea_number", length = 20)
    private String deaNumber;
}
