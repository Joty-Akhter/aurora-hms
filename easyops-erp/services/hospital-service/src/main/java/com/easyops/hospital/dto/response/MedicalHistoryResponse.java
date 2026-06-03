package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientMedicalHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryResponse {
    
    private UUID historyId;
    private UUID patientId;
    private PatientMedicalHistory.HistoryType historyType;
    private String conditionName;
    private String icd10Code;
    private String icd11Code;
    private String snomedCode;
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    private PatientMedicalHistory.Status status;
    private String severity;
    private String notes;
    private UUID documentedBy;
    private LocalDate documentedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
