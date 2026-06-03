package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionInteraction;
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
public class PrescriptionInteractionResponse {
    
    private UUID interactionId;
    private UUID prescriptionId;
    
    private String interactingMedication;
    private String interactingMedicationCode;
    private String interactionType;
    private PrescriptionInteraction.InteractionCategory interactionCategory;
    private PrescriptionInteraction.InteractionSeverity severity;
    private PrescriptionInteraction.ClinicalSignificanceLevel clinicalSignificanceLevel;
    
    private String description;
    private String clinicalSignificance;
    private String actionRequired;
    private String managementGuidance;
    
    private String mechanism;
    private String onsetTime;
    private String evidenceLevel;
    private String interactingSubstance;
    private String interactingSubstanceType;
    private String documentationReferences;
    
    private Boolean isAcknowledged;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedDate;
    private String overrideReason;
    
    private LocalDateTime createdAt;
}
