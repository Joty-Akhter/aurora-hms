package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionAllergyCheck;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyCheckResponse {
    
    private Boolean hasAllergies;
    private List<AllergyDetail> allergies;
    private String summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllergyDetail {
        private String allergenName;
        private String allergenType;
        private String reactionType;
        private PrescriptionAllergyCheck.AllergySeverity severity;
        /** FR-P1.7: How the match was found (DIRECT, SYNONYM, DRUG_COMPONENT, DRUG_CLASS, CROSS_REACTIVITY). */
        private String matchType;
        /** FR-P1.7: Prescriber-facing explanation of the match (e.g. cross-reactivity mechanism). */
        private String clinicalNote;
    }
}
