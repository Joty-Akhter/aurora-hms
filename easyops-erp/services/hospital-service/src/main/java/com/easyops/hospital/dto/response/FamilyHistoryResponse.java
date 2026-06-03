package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.FamilyHistory;
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
public class FamilyHistoryResponse {
    
    private UUID familyHistoryId;
    private UUID patientId;
    private FamilyHistory.FamilyMemberRelationship familyMemberRelationship;
    private String conditionName;
    private String icd10Code;
    private String icd11Code;
    private String snomedCode;
    private Integer ageAtOnset;
    private Integer ageAtDeath;
    private String notes;
    private LocalDate documentedDate;
    private UUID documentedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
