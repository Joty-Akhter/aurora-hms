package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.ImagingStudy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingStudyResponse {
    
    private UUID studyId;
    private UUID orderId;
    private String orderNumber;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private UUID encounterId;
    private UUID organizationId;
    
    // Study Identification
    private String studyNumber;
    private String accessionNumber;
    private String studyName;
    private ImagingStudy.StudyModality studyModality;
    private String cptCode;
    private LocalDateTime studyDate;
    private LocalDateTime studyCompletionDate;
    private ImagingStudy.StudyStatus studyStatus;
    
    // Study Details
    private String bodyPartExamined;
    private String laterality;
    private Integer numberOfImages;
    private Integer numberOfSeries;
    private Boolean contrastUsed;
    private String contrastType;
    private String techniqueProtocol;
    private String equipmentUsed;
    private String equipmentModel;
    private String radiationDose;
    private Integer studyDurationMinutes;
    
    // Radiologist Information
    private String interpretingRadiologistName;
    private String interpretingRadiologistNpi;
    private String interpretingRadiologistSpecialty;
    private String preliminaryReadingBy;
    private String reviewingRadiologist;
    private LocalDateTime reportDate;
    private LocalDateTime reportFinalizedDate;
    
    // Report Content
    private String clinicalHistory;
    private String techniqueDescription;
    private String findings;
    private String impressionConclusion;
    private String recommendations;
    private String urgencyIndicator;
    
    // Report Status
    private Boolean isPreliminary;
    private Boolean isFinal;
    private Boolean isAddendum;
    private Boolean isAmended;
    private Boolean isCancelled;
    
    // Critical Finding Management
    private Boolean hasCriticalFindings;
    private Boolean isCriticalFindingAcknowledged;
    private UUID criticalFindingAcknowledgedBy;
    private LocalDateTime criticalFindingAcknowledgedDate;
    private String criticalFindingResponse;
    
    // Report Review
    private Boolean isReviewed;
    private UUID reviewedBy;
    private LocalDateTime reviewedDate;
    private String reviewNotes;
    
    // Report History
    private UUID originalStudyId;
    private String correctionReason;
    private String amendmentReason;
    private String addendumReason;
    private String cancellationReason;
    private LocalDateTime correctionDate;
    private LocalDateTime amendmentDate;
    private LocalDateTime addendumDate;
    private LocalDateTime cancellationDate;
    
    // DICOM Information
    private String dicomStudyInstanceUid;
    private String dicomSeriesInstanceUid;
    private String dicomStorageLocation;
    private Boolean pacsIntegrated;
    private Boolean imagesAvailable;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
