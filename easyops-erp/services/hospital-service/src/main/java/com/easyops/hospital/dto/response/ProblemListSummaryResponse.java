package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemListSummaryResponse {
    
    private Long totalProblems;
    private Long activeProblems;
    private Long resolvedProblems;
    private Long chronicProblems;
    
    private List<PatientProblemResponse> activeProblemsList;
    private List<PatientProblemResponse> resolvedProblemsList;
    private List<PatientProblemResponse> highPriorityProblems;
}
