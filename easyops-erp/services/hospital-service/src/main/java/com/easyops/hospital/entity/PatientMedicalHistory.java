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
@Table(name = "patient_medical_history", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientMedicalHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    private UUID historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "history_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private HistoryType historyType;
    
    @Column(name = "condition_name", nullable = false, length = 200)
    private String conditionName;
    
    @Column(name = "icd10_code", length = 20)
    private String icd10Code;
    
    @Column(name = "icd11_code", length = 20)
    private String icd11Code;
    
    @Column(name = "snomed_code", length = 50)
    private String snomedCode;
    
    @Column(name = "onset_date")
    private LocalDate onsetDate;
    
    @Column(name = "resolution_date")
    private LocalDate resolutionDate;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;
    
    @Column(name = "severity", length = 20)
    private String severity;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "documented_by")
    private UUID documentedBy;
    
    @Column(name = "documented_date")
    @Builder.Default
    private LocalDate documentedDate = LocalDate.now();
    
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
    
    public enum HistoryType {
        PAST_MEDICAL, FAMILY, SOCIAL, IMMUNIZATION
    }
    
    public enum Status {
        ACTIVE, RESOLVED, CHRONIC, INACTIVE
    }
}
