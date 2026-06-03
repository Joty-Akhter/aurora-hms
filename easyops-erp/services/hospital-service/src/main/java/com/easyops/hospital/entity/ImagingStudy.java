package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imaging_studies", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImagingStudy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "study_id")
    private UUID studyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ImagingOrder order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "encounter_id")
    private UUID encounterId;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    // Study Identification
    @Column(name = "study_number", unique = true, nullable = false, length = 100)
    private String studyNumber;
    
    @Column(name = "accession_number", unique = true, nullable = false, length = 100)
    private String accessionNumber;
    
    @Column(name = "study_name", nullable = false, length = 500)
    private String studyName;
    
    @Column(name = "study_modality", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StudyModality studyModality;
    
    @Column(name = "cpt_code", nullable = false, length = 20)
    private String cptCode;
    
    @Column(name = "study_date", nullable = false)
    private LocalDateTime studyDate;
    
    @Column(name = "study_completion_date", nullable = false)
    private LocalDateTime studyCompletionDate;
    
    @Column(name = "study_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StudyStatus studyStatus = StudyStatus.COMPLETED;
    
    // Study Details
    @Column(name = "body_part_examined", nullable = false, length = 200)
    private String bodyPartExamined;
    
    @Column(name = "laterality", length = 50)
    private String laterality;
    
    @Column(name = "number_of_images")
    private Integer numberOfImages;
    
    @Column(name = "number_of_series")
    private Integer numberOfSeries;
    
    @Column(name = "contrast_used")
    private Boolean contrastUsed = false;
    
    @Column(name = "contrast_type", length = 100)
    private String contrastType;
    
    @Column(name = "technique_protocol", length = 500)
    private String techniqueProtocol;
    
    @Column(name = "equipment_used", length = 200)
    private String equipmentUsed;
    
    @Column(name = "equipment_model", length = 200)
    private String equipmentModel;
    
    @Column(name = "radiation_dose", length = 100)
    private String radiationDose;
    
    @Column(name = "study_duration_minutes")
    private Integer studyDurationMinutes;
    
    // Radiologist Information
    @Column(name = "interpreting_radiologist_name", length = 200)
    private String interpretingRadiologistName;
    
    @Column(name = "interpreting_radiologist_npi", length = 20)
    private String interpretingRadiologistNpi;
    
    @Column(name = "interpreting_radiologist_specialty", length = 100)
    private String interpretingRadiologistSpecialty;
    
    @Column(name = "preliminary_reading_by", length = 200)
    private String preliminaryReadingBy;
    
    @Column(name = "reviewing_radiologist", length = 200)
    private String reviewingRadiologist;
    
    @Column(name = "report_date")
    private LocalDateTime reportDate;
    
    @Column(name = "report_finalized_date")
    private LocalDateTime reportFinalizedDate;
    
    // Report Content
    @Column(name = "clinical_history", columnDefinition = "TEXT")
    private String clinicalHistory;
    
    @Column(name = "technique_description", columnDefinition = "TEXT")
    private String techniqueDescription;
    
    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;
    
    @Column(name = "impression_conclusion", columnDefinition = "TEXT")
    private String impressionConclusion;
    
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;
    
    @Column(name = "urgency_indicator", length = 50)
    private String urgencyIndicator;
    
    // Report Status
    @Column(name = "is_preliminary")
    private Boolean isPreliminary = false;
    
    @Column(name = "is_final")
    private Boolean isFinal = false;
    
    @Column(name = "is_addendum")
    private Boolean isAddendum = false;
    
    @Column(name = "is_amended")
    private Boolean isAmended = false;
    
    @Column(name = "is_cancelled")
    private Boolean isCancelled = false;
    
    // Critical Finding Management
    @Column(name = "has_critical_findings")
    private Boolean hasCriticalFindings = false;
    
    @Column(name = "is_critical_finding_acknowledged")
    private Boolean isCriticalFindingAcknowledged = false;
    
    @Column(name = "critical_finding_acknowledged_by")
    private UUID criticalFindingAcknowledgedBy;
    
    @Column(name = "critical_finding_acknowledged_date")
    private LocalDateTime criticalFindingAcknowledgedDate;
    
    @Column(name = "critical_finding_response", columnDefinition = "TEXT")
    private String criticalFindingResponse;
    
    // Report Review
    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;
    
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;
    
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    // Report History
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_study_id")
    private ImagingStudy originalStudy;
    
    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;
    
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;
    
    @Column(name = "addendum_reason", columnDefinition = "TEXT")
    private String addendumReason;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "correction_date")
    private LocalDateTime correctionDate;
    
    @Column(name = "amendment_date")
    private LocalDateTime amendmentDate;
    
    @Column(name = "addendum_date")
    private LocalDateTime addendumDate;
    
    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;
    
    // DICOM Information
    @Column(name = "dicom_study_instance_uid", length = 200)
    private String dicomStudyInstanceUid;
    
    @Column(name = "dicom_series_instance_uid", length = 200)
    private String dicomSeriesInstanceUid;
    
    @Column(name = "dicom_storage_location", length = 500)
    private String dicomStorageLocation;
    
    @Column(name = "pacs_integrated")
    private Boolean pacsIntegrated = false;
    
    @Column(name = "images_available")
    private Boolean imagesAvailable = false;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public enum StudyModality {
        XRAY, CT, MRI, ULTRASOUND, MAMMOGRAPHY, NUCLEAR_MEDICINE, PET, DEXA, FLUOROSCOPY, OTHER
    }
    
    public enum StudyStatus {
        COMPLETED, PRELIMINARY, FINAL, CANCELLED, AMENDED
    }
}
