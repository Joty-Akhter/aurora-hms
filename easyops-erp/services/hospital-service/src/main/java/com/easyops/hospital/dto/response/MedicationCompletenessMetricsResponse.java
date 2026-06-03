package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationCompletenessMetricsResponse {
    private UUID patientId;
    private String patientName;
    private LocalDate reportDate;
    private BigDecimal completenessScore; // 0-100
    private Integer totalMedications;
    private Integer completeMedications;
    private Integer incompleteMedications;
    private List<CompletenessDetail> completenessDetails;
    private Map<String, Integer> missingFieldCounts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletenessDetail {
        private UUID medicationId;
        private String medicationName;
        private BigDecimal completenessScore; // 0-100
        private List<String> missingFields;
        private List<String> incompleteFields;
    }
}
