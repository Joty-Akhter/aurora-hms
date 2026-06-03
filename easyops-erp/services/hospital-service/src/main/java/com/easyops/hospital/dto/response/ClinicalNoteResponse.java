package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.ClinicalNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteResponse {
    
    private UUID noteId;
    private UUID patientId;
    private UUID encounterId;
    
    // Note Classification
    private ClinicalNote.NoteType noteType;
    private LocalDate noteDate;
    private LocalTime noteTime;
    
    // SOAP Note Components
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    
    // Additional Note Fields
    private String chiefComplaint;
    private String reviewOfSystems;
    private String physicalExamination;
    private String clinicalImpression;
    private String treatmentPlan;
    private String followUpInstructions;
    
    // Note Status and Workflow
    private ClinicalNote.NoteStatus noteStatus;
    
    // Authoring Information
    private UUID createdBy;
    private LocalDateTime createdDate;
    
    // Signing Information
    private UUID signedBy;
    private LocalDateTime signedDate;
    private ClinicalNote.SignatureMethod signatureMethod;
    
    // Amendment Information
    private UUID amendedBy;
    private LocalDateTime amendedDate;
    private String amendmentReason;
    private UUID originalNoteId;
    
    // Versioning
    private Integer versionNumber;
    private Boolean isCurrentVersion;
    
    // Additional Metadata
    private String specialty;
    private UUID departmentId;
    private UUID locationId;
    private String visitType;
    private String notes;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    
    // Related Data
    private List<NoteAttachmentResponse> attachments;
    private Integer amendmentCount;
    private List<ClinicalNoteMedicationResponse> medications;
    private Integer medicationCount;
}
