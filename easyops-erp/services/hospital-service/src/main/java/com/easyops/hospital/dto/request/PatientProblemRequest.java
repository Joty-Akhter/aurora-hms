package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PatientProblem;
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
public class PatientProblemRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    @NotBlank(message = "Problem name is required")
    private String problemName;
    
    private String icd10Code;
    private String icd11Code;
    private String snomedCode;
    
    @NotNull(message = "Problem type is required")
    private PatientProblem.ProblemType problemType;
    
    private PatientProblem.ProblemStatus status;
    
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    
    private PatientProblem.Severity severity;
    private String chronicity;
    private PatientProblem.Priority priority;
    
    private String resolutionNotes;
    private String notes;
}
