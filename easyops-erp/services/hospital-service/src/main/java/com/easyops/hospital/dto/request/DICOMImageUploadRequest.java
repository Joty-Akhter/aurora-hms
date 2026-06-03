package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for DICOM image upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DICOMImageUploadRequest {
    
    private UUID studyId;
    private String description;
    private Boolean generateThumbnail;
}
