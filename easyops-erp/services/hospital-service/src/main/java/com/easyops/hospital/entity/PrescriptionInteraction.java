package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescription_interactions", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "interaction_id")
    private UUID interactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    // Interaction Information
    @Column(name = "interacting_medication", length = 500)
    private String interactingMedication;
    
    @Column(name = "interacting_medication_code", length = 100)
    private String interactingMedicationCode;
    
    @Column(name = "interaction_type", length = 100)
    private String interactionType;
    
    @Column(name = "interaction_category", length = 50)
    @Enumerated(EnumType.STRING)
    private InteractionCategory interactionCategory;
    
    @Column(name = "severity", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private InteractionSeverity severity;
    
    @Column(name = "clinical_significance_level", length = 50)
    @Enumerated(EnumType.STRING)
    private ClinicalSignificanceLevel clinicalSignificanceLevel;
    
    // Clinical Details
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "clinical_significance", columnDefinition = "TEXT")
    private String clinicalSignificance;
    
    @Column(name = "action_required", columnDefinition = "TEXT")
    private String actionRequired;
    
    @Column(name = "management_guidance", columnDefinition = "TEXT")
    private String managementGuidance;
    
    // Status
    @Column(name = "is_acknowledged")
    @Builder.Default
    private Boolean isAcknowledged = false;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;
    
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;
    
    // Additional Interaction Details
    @Column(name = "interacting_substance", length = 500)
    private String interactingSubstance; // For food, lab tests, etc.
    
    @Column(name = "interacting_substance_type", length = 50)
    private String interactingSubstanceType; // FOOD, LAB_TEST, HERBAL, etc.
    
    @Column(name = "mechanism", columnDefinition = "TEXT")
    private String mechanism; // Mechanism of interaction
    
    @Column(name = "onset_time", length = 100)
    private String onsetTime; // Time to onset of interaction
    
    @Column(name = "evidence_level", length = 50)
    private String evidenceLevel; // Quality of evidence (e.g., "Established", "Theoretical")
    
    @Column(name = "documentation_references", columnDefinition = "TEXT")
    private String documentationReferences; // References to supporting documentation
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum InteractionSeverity {
        CONTRAINDICATED, MAJOR, MODERATE, MINOR, UNKNOWN
    }
    
    public enum InteractionCategory {
        DRUG_DRUG,
        DRUG_FOOD,
        DRUG_LAB,
        DRUG_HERBAL,
        DRUG_ALCOHOL,
        DRUG_DISEASE,
        /** FDA-style pregnancy / lactation considerations (FR-P1.7). */
        PREGNANCY_LACTATION,
        /** Pediatric vs adult / geriatric dosing bands (FR-P1.7). */
        PEDIATRIC_GERIATRIC_DOSING,
        /** mg/kg or weight-scaled dosing heuristics (FR-P1.7). */
        WEIGHT_BASED_DOSING,
        /** Renal or hepatic dose-adjustment alerts (FR-P1.7). */
        RENAL_HEPATIC_ALERT,
        OTHER
    }
    
    public enum ClinicalSignificanceLevel {
        CRITICAL, SIGNIFICANT, MODERATE, MINOR, UNKNOWN
    }
}
