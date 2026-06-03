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
public class MedicationClinicalReportResponse {
    private UUID patientId;
    private String patientName;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reportType; // BY_PROVIDER, BY_PROBLEM
    private List<ProviderMedicationSummary> providerSummaries;
    private List<ProblemMedicationSummary> problemSummaries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderMedicationSummary {
        private UUID providerId;
        private String providerName;
        private String providerNpi;
        private Integer totalMedications;
        private Integer activeMedications;
        private List<MedicationResponse> medications;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemMedicationSummary {
        private UUID problemId;
        private String problemName;
        private String diagnosisCode;
        private Integer totalMedications;
        private Integer activeMedications;
        private List<MedicationResponse> medications;
    }
}
