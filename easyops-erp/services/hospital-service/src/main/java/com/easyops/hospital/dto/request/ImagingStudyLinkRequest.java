package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for linking imaging studies to other entities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingStudyLinkRequest {
    
    private UUID studyId;
    private UUID targetId; // ID of the entity to link to (note, problem, medication)
    private String linkType; // Type of link (REFERENCED, RELATED, etc.)
    private String linkStrength; // WEAK, MODERATE, STRONG
    private String clinicalRelevance;
    private String notes;
}
