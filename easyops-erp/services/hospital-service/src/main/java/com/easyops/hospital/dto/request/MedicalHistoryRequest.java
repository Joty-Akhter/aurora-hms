package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PatientMedicalHistory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryRequest {
    
    @NotNull(message = "History type is required")
    private PatientMedicalHistory.HistoryType historyType;
    
    @NotBlank(message = "Condition name is required")
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
}
