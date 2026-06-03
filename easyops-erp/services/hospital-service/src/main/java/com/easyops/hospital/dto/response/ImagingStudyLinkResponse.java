package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for imaging study links
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingStudyLinkResponse {
    
    private UUID linkId;
    private UUID studyId;
    private UUID targetId;
    private String targetType; // CLINICAL_NOTE, PROBLEM, MEDICATION
    private String linkType;
    private String linkStrength;
    private String clinicalRelevance;
    private String notes;
    private UUID linkedBy;
    private LocalDateTime linkedDate;
}
