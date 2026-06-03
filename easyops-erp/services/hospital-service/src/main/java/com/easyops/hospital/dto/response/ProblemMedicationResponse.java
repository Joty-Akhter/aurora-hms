package com.easyops.hospital.dto.response;

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
public class ProblemMedicationResponse {
    
    private UUID linkId;
    private UUID problemId;
    private UUID medicationId;
    private String medicationName;
    private String genericName;
    private String linkType;
    private String linkStrength;
    private String clinicalRelevance;
    private String notes;
    private UUID linkedBy;
    private LocalDateTime linkedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
