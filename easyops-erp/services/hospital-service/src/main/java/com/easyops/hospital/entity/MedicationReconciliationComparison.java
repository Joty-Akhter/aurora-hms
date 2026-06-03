package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_reconciliation_comparisons", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MedicationReconciliationComparison {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "comparison_id")
    private UUID comparisonId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private MedicationReconciliation reconciliation;
    
    // Medication Information
    @Column(name = "medication_name", nullable = false, length = 500)
    private String medicationName;
    
    @Column(name = "generic_name", length = 500)
    private String genericName;
    
    @Column(name = "medication_code", length = 100)
    private String medicationCode;
    
    @Column(name = "medication_code_type", length = 20)
    @Enumerated(EnumType.STRING)
    private Medication.MedicationCodeType medicationCodeType;
    
    // Comparison Details
    @Column(name = "comparison_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ComparisonStatus comparisonStatus;
    
    @Column(name = "action_taken", length = 50)
    @Enumerated(EnumType.STRING)
    private ActionTaken actionTaken;
    
    // Source Information
    @Column(name = "source_medication_id")
    private UUID sourceMedicationId;
    
    @Column(name = "target_medication_id")
    private UUID targetMedicationId;
    
    // Comparison Data (before/after)
    @Column(name = "before_dosage_strength", precision = 10, scale = 3)
    private BigDecimal beforeDosageStrength;
    
    @Column(name = "after_dosage_strength", precision = 10, scale = 3)
    private BigDecimal afterDosageStrength;
    
    @Column(name = "before_dosage_unit", length = 50)
    private String beforeDosageUnit;
    
    @Column(name = "after_dosage_unit", length = 50)
    private String afterDosageUnit;
    
    @Column(name = "before_frequency", length = 200)
    private String beforeFrequency;
    
    @Column(name = "after_frequency", length = 200)
    private String afterFrequency;
    
    @Column(name = "before_route", length = 50)
    private String beforeRoute;
    
    @Column(name = "after_route", length = 50)
    private String afterRoute;
    
    @Column(name = "before_instructions", columnDefinition = "TEXT")
    private String beforeInstructions;
    
    @Column(name = "after_instructions", columnDefinition = "TEXT")
    private String afterInstructions;
    
    // Differences
    @Column(name = "differences", columnDefinition = "TEXT")
    private String differences;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    public enum ComparisonStatus {
        NEW, CHANGED, DISCONTINUED, UNCHANGED, CONFLICT
    }
    
    public enum ActionTaken {
        ADDED, MODIFIED, DISCONTINUED, KEPT, RESOLVED, PENDING
    }
}
