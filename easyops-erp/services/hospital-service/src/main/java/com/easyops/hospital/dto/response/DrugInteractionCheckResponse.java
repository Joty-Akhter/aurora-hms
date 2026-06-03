package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionInteraction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteractionCheckResponse {
    
    private Boolean hasInteractions;
    private List<InteractionDetail> interactions;
    private String summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractionDetail {
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
    }
}
