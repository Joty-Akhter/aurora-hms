package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Medication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationListReportResponse {
    private UUID patientId;
    private String patientName;
    private String reportType; // COMPLETE, CURRENT, HISTORICAL
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalMedications;
    private Integer activeMedications;
    private Integer discontinuedMedications;
    private Integer onHoldMedications;
    private Integer completedMedications;
    private List<MedicationResponse> medications;
    private Map<String, Object> summary;
}
