package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medication_reconciliation", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MedicationReconciliation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "reconciliation_id")
    private UUID reconciliationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    // Reconciliation Information
    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;
    
    @Column(name = "reconciliation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ReconciliationType reconciliationType;
    
    @Column(name = "reconciliation_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.IN_PROGRESS;
    
    // Reconciliation Context
    @Column(name = "performed_by")
    private UUID performedBy;
    
    @Column(name = "performed_by_name", length = 200)
    private String performedByName;
    
    @Column(name = "verified_by")
    private UUID verifiedBy;
    
    @Column(name = "verified_by_name", length = 200)
    private String verifiedByName;
    
    @Column(name = "verification_date")
    private LocalDateTime verificationDate;
    
    // Reconciliation Summary
    @Column(name = "total_medications_before")
    @Builder.Default
    private Integer totalMedicationsBefore = 0;
    
    @Column(name = "total_medications_after")
    @Builder.Default
    private Integer totalMedicationsAfter = 0;
    
    @Column(name = "medications_added")
    @Builder.Default
    private Integer medicationsAdded = 0;
    
    @Column(name = "medications_modified")
    @Builder.Default
    private Integer medicationsModified = 0;
    
    @Column(name = "medications_discontinued")
    @Builder.Default
    private Integer medicationsDiscontinued = 0;
    
    @Column(name = "medications_unchanged")
    @Builder.Default
    private Integer medicationsUnchanged = 0;
    
    // Notes and Documentation
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "reconciliation_summary", columnDefinition = "TEXT")
    private String reconciliationSummary;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    // Relationships
    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationReconciliationSource> sources;
    
    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationReconciliationComparison> comparisons;
    
    public enum ReconciliationType {
        ADMISSION, DISCHARGE, TRANSFER, ENCOUNTER, MANUAL
    }
    
    public enum ReconciliationStatus {
        IN_PROGRESS, COMPLETED, CANCELLED
    }
}
