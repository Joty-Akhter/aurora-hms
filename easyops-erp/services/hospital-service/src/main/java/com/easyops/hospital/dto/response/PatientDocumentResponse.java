package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PatientDocumentResponse {

    private UUID documentId;
    private UUID patientId;
    private UUID organizationId;
    private UUID encounterId;
    private UUID clinicalNoteId;
    private UUID labResultId;
    private UUID prescriptionId;

    private PatientDocument.DocumentType documentType;
    private String documentCategory;
    private String title;
    private String description;

    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;

    private String sourceFacility;
    private LocalDateTime documentDate;

    private UUID uploadedBy;
    private LocalDateTime uploadedDate;

    private Boolean isActive;
    private Boolean isConfidential;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
