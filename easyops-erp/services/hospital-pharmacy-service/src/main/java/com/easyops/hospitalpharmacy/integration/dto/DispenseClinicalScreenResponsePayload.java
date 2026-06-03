package com.easyops.hospitalpharmacy.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors hospital-service {@code DispenseClinicalScreenResponse} JSON for RestTemplate deserialization.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DispenseClinicalScreenResponsePayload {

    private DrugInteractionPayload interactions;
    private AllergyCheckPayload allergies;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrugInteractionPayload {
        private Boolean hasInteractions;
        private List<InteractionDetailPayload> interactions;
        private String summary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InteractionDetailPayload {
        /** {@code CONTRAINDICATED}, {@code MAJOR}, etc. */
        private String severity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllergyCheckPayload {
        private Boolean hasAllergies;
        private List<AllergyDetailPayload> allergies;
        private String summary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllergyDetailPayload {
        /** e.g. CROSS_REACTIVITY, DIRECT, DRUG_CLASS */
        private String matchType;
    }
}
