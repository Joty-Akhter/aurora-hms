package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionAllergyCheck;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionAllergyCheckResponse {
    
    private UUID checkId;
    private UUID prescriptionId;
    
    private String allergenName;
    private String allergenCode;
    private String allergenType;
    private String reactionType;
    private PrescriptionAllergyCheck.AllergySeverity severity;
    
    private PrescriptionAllergyCheck.ActionTaken actionTaken;
    private String overrideReason;
    private UUID overrideBy;
    private LocalDateTime overrideDate;
    
    private Boolean isAcknowledged;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedDate;

    /** FR-P1.7: How the match was found (DIRECT, SYNONYM, DRUG_COMPONENT, DRUG_CLASS, CROSS_REACTIVITY). */
    private String matchType;
    /** FR-P1.7: Prescriber-facing explanation of the match. */
    private String clinicalNote;

    private LocalDateTime createdAt;
}
