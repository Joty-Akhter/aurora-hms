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
@Table(name = "patient_documents", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "organization_id")
    private UUID organizationId;

    // Context linkage (optional — document may also belong to an encounter/note/order)
    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "clinical_note_id")
    private UUID clinicalNoteId;

    @Column(name = "lab_result_id")
    private UUID labResultId;

    @Column(name = "prescription_id")
    private UUID prescriptionId;

    // Document classification
    @Column(name = "document_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "document_category", length = 100)
    private String documentCategory;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // File metadata
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_hash", length = 255)
    private String fileHash;

    // Source / provenance
    @Column(name = "source_facility", length = 255)
    private String sourceFacility;

    @Column(name = "document_date")
    private LocalDateTime documentDate;

    // Authoring
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_date")
    @Builder.Default
    private LocalDateTime uploadedDate = LocalDateTime.now();

    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_confidential")
    @Builder.Default
    private Boolean isConfidential = false;

    // Audit
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DocumentType {
        // Clinical
        PATHOLOGY_REPORT,       // Histopathology / biopsy results
        RADIOLOGY_REPORT,       // Radiology / imaging narrative report
        LAB_REPORT,             // External laboratory results
        CLINICAL_PHOTO,         // Wound photos, skin lesions, clinical images
        SURGICAL_REPORT,        // Operative / procedure notes

        // Orders & Prescriptions
        PRESCRIPTION,           // Scanned or PDF prescription
        REFERRAL_LETTER,        // Referral to specialist / facility
        DISCHARGE_SUMMARY,      // Discharge / transfer summary

        // Administrative
        CONSENT_FORM,           // Signed patient consent
        INSURANCE_DOCUMENT,     // Insurance card, pre-auth, EOB
        VITAL_RECORDS,          // Birth certificate, death certificate
        IDENTITY_DOCUMENT,      // National ID, passport scan
        EXTERNAL_RECORD,        // Records from another facility
        ADVANCE_DIRECTIVE,      // Living will, POLST, DNR

        OTHER
    }
}
