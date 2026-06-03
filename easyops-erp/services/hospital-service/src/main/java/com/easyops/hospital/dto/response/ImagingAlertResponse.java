package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.ImagingCriticalFindingAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for imaging alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingAlertResponse {
    
    private UUID alertId;
    private UUID studyId;
    private UUID patientId;
    private UUID orderId;
    private ImagingCriticalFindingAlert.AlertStatus alertStatus;
    private ImagingCriticalFindingAlert.AlertPriority alertPriority;
    private String alertMessage;
    private String findingKeywords;
    private Boolean isAcknowledged;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedDate;
    private String acknowledgmentNotes;
    private LocalDateTime notificationSentDate;
    private Boolean notificationDelivered;
    private LocalDateTime createdAt;
}
