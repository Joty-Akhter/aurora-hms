package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.ImagingStudy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ImagingStudyRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private UUID patientId;
    private UUID encounterId;
    
    // Study Identification
    private String studyNumber;
    
    @NotBlank(message = "Accession number is required")
    private String accessionNumber;
    
    @NotBlank(message = "Study name is required")
    private String studyName;
    
    @NotNull(message = "Study modality is required")
    private ImagingStudy.StudyModality studyModality;
    
    @NotBlank(message = "CPT code is required")
    private String cptCode;
    
    @NotNull(message = "Study date is required")
    private LocalDateTime studyDate;
    
    @NotNull(message = "Study completion date is required")
    private LocalDateTime studyCompletionDate;
    
    private ImagingStudy.StudyStatus studyStatus = ImagingStudy.StudyStatus.COMPLETED;
    
    // Study Details
    @NotBlank(message = "Body part examined is required")
    private String bodyPartExamined;
    
    private String laterality;
    private Integer numberOfImages;
    private Integer numberOfSeries;
    private Boolean contrastUsed = false;
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
    private Boolean isPreliminary = false;
    private Boolean isFinal = false;
    private Boolean isAddendum = false;
    private Boolean isAmended = false;
    private Boolean isCancelled = false;
    
    // Critical Finding Management
    private Boolean hasCriticalFindings = false;
    private Boolean isCriticalFindingAcknowledged = false;
    private String criticalFindingResponse;
    
    // Report Review
    private Boolean isReviewed = false;
    private String reviewNotes;
    
    // Report History
    private UUID originalStudyId;
    private String correctionReason;
    private String amendmentReason;
    private String addendumReason;
    private String cancellationReason;
    
    // DICOM Information
    private String dicomStudyInstanceUid;
    private String dicomSeriesInstanceUid;
    private String dicomStorageLocation;
    private Boolean pacsIntegrated = false;
    private Boolean imagesAvailable = false;
}
