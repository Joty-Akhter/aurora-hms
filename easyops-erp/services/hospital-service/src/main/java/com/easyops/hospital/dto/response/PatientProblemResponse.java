package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientProblem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProblemResponse {
    
    private UUID problemId;
    private UUID patientId;
    private UUID encounterId;
    
    // Problem Identification
    private String problemName;
    private String icd10Code;
    private String icd11Code;
    private String snomedCode;
    
    // Problem Classification
    private PatientProblem.ProblemType problemType;
    private PatientProblem.ProblemStatus status;
    
    // Dates
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    
    // Clinical Details
    private PatientProblem.Severity severity;
    private String chronicity;
    private PatientProblem.Priority priority;
    
    // Documentation
    private UUID documentedBy;
    private LocalDate documentedDate;
    
    // Resolution Information
    private UUID resolvedBy;
    private LocalDate resolvedDate;
    private String resolutionNotes;
    
    // Additional Information
    private String notes;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    // Related Data
    private List<ProblemHistoryResponse> history;
    private Integer historyCount;
}
