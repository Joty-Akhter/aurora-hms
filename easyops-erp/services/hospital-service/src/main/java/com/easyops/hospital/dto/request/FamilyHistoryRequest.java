package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.FamilyHistory;
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
public class FamilyHistoryRequest {
    
    @NotNull(message = "Family member relationship is required")
    private FamilyHistory.FamilyMemberRelationship familyMemberRelationship;
    
    @NotBlank(message = "Condition name is required")
    private String conditionName;
    
    private String icd10Code;
    private String icd11Code;
    private String snomedCode;
    private Integer ageAtOnset;
    private Integer ageAtDeath;
    private String notes;
    private LocalDate documentedDate;
    private UUID documentedBy;
}
