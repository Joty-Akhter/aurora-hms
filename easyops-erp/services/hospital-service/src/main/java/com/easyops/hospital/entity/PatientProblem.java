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
@Table(name = "patient_problems", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientProblem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "problem_id")
    private UUID problemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    // Problem Identification
    @Column(name = "problem_name", nullable = false, length = 500)
    private String problemName;
    
    @Column(name = "icd10_code", length = 20)
    private String icd10Code;
    
    @Column(name = "icd11_code", length = 20)
    private String icd11Code;
    
    @Column(name = "snomed_code", length = 50)
    private String snomedCode;
    
    // Problem Classification
    @Column(name = "problem_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProblemType problemType;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProblemStatus status = ProblemStatus.ACTIVE;
    
    // Dates
    @Column(name = "onset_date")
    private LocalDate onsetDate;
    
    @Column(name = "resolution_date")
    private LocalDate resolutionDate;
    
    // Clinical Details
    @Column(name = "severity", length = 50)
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "chronicity", length = 50)
    private String chronicity;
    
    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    // Documentation
    @Column(name = "documented_by", nullable = false)
    private UUID documentedBy;
    
    @Column(name = "documented_date")
    @Builder.Default
    private LocalDate documentedDate = LocalDate.now();
    
    // Resolution Information
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "resolved_date")
    private LocalDate resolvedDate;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    // Additional Information
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
    
    // Relationships
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemHistory> history;
    
    public enum ProblemType {
        DIAGNOSIS, SYMPTOM, FINDING, CONDITION, ALLERGY, OTHER
    }
    
    public enum ProblemStatus {
        ACTIVE, RESOLVED, INACTIVE, RULED_OUT, CHRONIC, REMISSION
    }
    
    public enum Severity {
        MILD, MODERATE, SEVERE, CRITICAL
    }
    
    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}
