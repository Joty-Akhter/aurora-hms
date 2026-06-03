package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIndicationReportResponse {
    private UUID patientId;
    private String patientName;
    private String indication;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalMedications;
    private List<MedicationResponse> medications;
    private List<IndicationSummary> indicationSummaries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicationSummary {
        private String indication;
        private Integer medicationCount;
        private Integer activeCount;
        private Integer discontinuedCount;
    }
}
