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
@Table(name = "prescription_allergy_checks", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionAllergyCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "check_id")
    private UUID checkId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    // Allergy Information
    @Column(name = "allergen_name", nullable = false, length = 500)
    private String allergenName;
    
    @Column(name = "allergen_code", length = 100)
    private String allergenCode;
    
    @Column(name = "allergen_type", length = 50)
    private String allergenType;
    
    @Column(name = "reaction_type", length = 200)
    private String reactionType;
    
    @Column(name = "severity", length = 50)
    @Enumerated(EnumType.STRING)
    private AllergySeverity severity;
    
    // Action Taken
    @Column(name = "action_taken", length = 100)
    @Enumerated(EnumType.STRING)
    private ActionTaken actionTaken;
    
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;
    
    @Column(name = "override_by")
    private UUID overrideBy;
    
    @Column(name = "override_date")
    private LocalDateTime overrideDate;
    
    // FR-P1.7 Phase 1: allergy match metadata
    @Column(name = "match_type", length = 30)
    private String matchType;

    @Column(name = "clinical_note", columnDefinition = "TEXT")
    private String clinicalNote;

    // Status
    @Column(name = "is_acknowledged")
    @Builder.Default
    private Boolean isAcknowledged = false;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum AllergySeverity {
        MILD, MODERATE, SEVERE, LIFE_THREATENING
    }
    
    public enum ActionTaken {
        OVERRIDDEN, CANCELLED, SUBSTITUTED, MONITORED, NO_ACTION
    }
}
