package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR-P1.4a: One row per ICD-10 diagnosis linked to a prescription.
 * Exactly one row per prescription should have {@code isPrimary = true}.
 */
@Entity
@Table(name = "prescription_diagnoses", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "diagnosis_code", nullable = false, length = 20)
    private String diagnosisCode;

    @Column(name = "diagnosis_description", length = 500)
    private String diagnosisDescription;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private Integer sequenceOrder = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
