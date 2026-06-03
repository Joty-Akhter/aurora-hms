package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAdherenceReportResponse {
    private UUID patientId;
    private String patientName;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal overallAdherenceRate; // Percentage
    private Integer totalMedications;
    private Integer adherentMedications;
    private Integer nonAdherentMedications;
    private List<MedicationAdherenceDetail> medicationDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationAdherenceDetail {
        private UUID medicationId;
        private String medicationName;
        private String indication;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal adherenceRate; // Percentage
        private Integer expectedDoses;
        private Integer actualDoses;
        private Integer missedDoses;
        private String adherenceStatus; // ADHERENT, PARTIAL, NON_ADHERENT
    }
}
