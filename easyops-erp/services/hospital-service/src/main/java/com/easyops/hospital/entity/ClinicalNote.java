package com.easyops.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinical_notes", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(
        ignoreUnknown = true,
        value = {"patient", "originalNote", "amendments", "attachments"})
public class ClinicalNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "note_id")
    private UUID noteId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    // Note Classification
    @Column(name = "note_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NoteType noteType;
    
    // Date and Time
    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;
    
    @Column(name = "note_time", nullable = false)
    private LocalTime noteTime;
    
    // SOAP Note Components
    @Column(name = "subjective", columnDefinition = "TEXT")
    private String subjective;
    
    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;
    
    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;
    
    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;
    
    // Additional Note Fields
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "review_of_systems", columnDefinition = "TEXT")
    private String reviewOfSystems;
    
    @Column(name = "physical_examination", columnDefinition = "TEXT")
    private String physicalExamination;
    
    @Column(name = "clinical_impression", columnDefinition = "TEXT")
    private String clinicalImpression;
    
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;
    
    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;
    
    // Note Status and Workflow
    @Column(name = "note_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NoteStatus noteStatus = NoteStatus.DRAFT;
    
    // Authoring Information
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    
    @Column(name = "created_date")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "authoring_provider_id")
    private UUID authoringProviderId;

    @Column(name = "authoring_provider_name", length = 200)
    private String authoringProviderName;
    
    // Signing Information
    @Column(name = "signed_by")
    private UUID signedBy;
    
    @Column(name = "signed_date")
    private LocalDateTime signedDate;
    
    @Column(name = "signature_method", length = 50)
    @Enumerated(EnumType.STRING)
    private SignatureMethod signatureMethod;
    
    // Amendment Information
    @Column(name = "amended_by")
    private UUID amendedBy;
    
    @Column(name = "amended_date")
    private LocalDateTime amendedDate;
    
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_note_id")
    private ClinicalNote originalNote;
    
    // Versioning
    @Column(name = "version_number")
    @Builder.Default
    private Integer versionNumber = 1;
    
    @Column(name = "is_current_version")
    @Builder.Default
    private Boolean isCurrentVersion = true;
    
    // Additional Metadata
    @Column(name = "specialty", length = 100)
    private String specialty;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "visit_type", length = 50)
    private String visitType;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    // Relationships
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteAttachment> attachments;
    
    @OneToMany(mappedBy = "originalNote", cascade = CascadeType.ALL)
    private List<ClinicalNote> amendments;
    
    public enum NoteType {
        SOAP, PROGRESS, CONSULTATION, DISCHARGE, PROCEDURE, ADMISSION, OPERATIVE, DOCTOR_NOTE, OTHER
    }
    
    public enum NoteStatus {
        DRAFT, FINAL, AMENDED, CORRECTED, VOIDED, SIGNED
    }
    
    public enum SignatureMethod {
        ELECTRONIC, DIGITAL, TYPED, VOICE, OTHER
    }
}
