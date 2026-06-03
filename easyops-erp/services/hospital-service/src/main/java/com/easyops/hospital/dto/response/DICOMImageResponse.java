package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for DICOM image information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DICOMImageResponse {
    
    private UUID attachmentId;
    private UUID studyId;
    private String fileName;
    private Long fileSize;
    private String fileUrl;
    private Boolean isDicom;
    private String dicomSeriesInstanceUid;
    private String dicomSopInstanceUid;
    private String thumbnailPath;
    private String thumbnailUrl;
    private LocalDateTime uploadedDate;
    
    // Compression information
    private Double compressionRatio;
    private Boolean compressed;
    
    // Network operation results
    private String studyInstanceUID;
    private String patientId;
    private String patientName;
    private String studyDate;
    private String accessionNumber;
    private String studyDescription;
    private String modality;
    private Boolean networkSuccess;
    private Integer networkStatus;
    private String networkMessage;
    private String networkSopInstanceUID;
}
