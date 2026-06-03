package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalValueDocumentationResponse {
    
    private UUID resultId;
    private String testName;
    private String resultValue;
    private String resultUnits;
    private LocalDateTime criticalValueDetectedAt;
    private Boolean isAcknowledged;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedDate;
    private String providerResponse;
    private List<CriticalValueAlertResponse> alertHistory;
    private List<DocumentationEntry> documentationHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentationEntry {
        private LocalDateTime timestamp;
        private String action;
        private UUID performedBy;
        private String description;
    }
}
