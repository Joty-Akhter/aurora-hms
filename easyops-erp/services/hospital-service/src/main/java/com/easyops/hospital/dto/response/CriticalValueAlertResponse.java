package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.LabCriticalValueAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalValueAlertResponse {
    
    private UUID alertId;
    private UUID resultId;
    private String resultNumber;
    private UUID patientId;
    private String patientName;
    private String mrn;
    private String testName;
    private String loincCode;
    private String resultValue;
    private String resultUnits;
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String alertMessage;
    private LabCriticalValueAlert.AlertStatus alertStatus;
    private LabCriticalValueAlert.AlertPriority alertPriority;
    private UUID notifiedProviderId;
    private String notifiedProviderName;
    private Boolean isAcknowledged;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedDate;
    private String providerResponse;
    private Integer escalationLevel;
    private UUID escalatedTo;
    private LocalDateTime escalationDate;
    private String escalationReason;
    private LocalDateTime notificationSentDate;
    private LocalDateTime createdAt;
}
