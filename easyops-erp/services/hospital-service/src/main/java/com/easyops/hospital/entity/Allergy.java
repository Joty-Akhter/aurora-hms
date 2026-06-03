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
import java.util.UUID;

@Entity
@Table(name = "allergies", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Allergy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "allergy_id")
    private UUID allergyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "allergen_name", nullable = false, length = 200)
    private String allergenName;
    
    @Column(name = "allergen_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AllergenType allergenType;
    
    @Column(name = "allergen_code", length = 50)
    private String allergenCode;
    
    @Column(name = "reaction_type", length = 100)
    private String reactionType;
    
    @Column(name = "severity", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "onset_date")
    private LocalDate onsetDate;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    @Column(name = "verification_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNCONFIRMED;
    
    @Column(name = "documented_by")
    private UUID documentedBy;
    
    @Column(name = "documented_date")
    @Builder.Default
    private LocalDate documentedDate = LocalDate.now();
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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
    
    public enum AllergenType {
        DRUG, FOOD, ENVIRONMENTAL, LATEX, OTHER
    }
    
    public enum Severity {
        MILD, MODERATE, SEVERE, LIFE_THREATENING
    }
    
    public enum Status {
        ACTIVE, RESOLVED, UNKNOWN
    }
    
    public enum VerificationStatus {
        CONFIRMED, UNCONFIRMED, REFUTED
    }
}
