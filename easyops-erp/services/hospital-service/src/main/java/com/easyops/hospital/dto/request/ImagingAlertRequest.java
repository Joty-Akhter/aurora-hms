package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.ImagingCriticalFindingAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating imaging alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingAlertRequest {
    
    private UUID studyId;
    private ImagingCriticalFindingAlert.AlertPriority alertPriority;
    private String alertMessage;
    private String findingKeywords;
}
