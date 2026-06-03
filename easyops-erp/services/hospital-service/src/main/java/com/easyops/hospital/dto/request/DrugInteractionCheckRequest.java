package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteractionCheckRequest {
    
    private String medicationCode; // RxNorm or NDC
    private String medicationName;
    private List<String> existingMedicationCodes; // List of current medications

    /**
     * Optional overrides for FR-P1.7 screening when not yet in chart (or to supplement chart).
     */
    private BigDecimal weightKg;
    /** Single line strength for weight-based heuristic (e.g. mg per tablet). */
    private BigDecimal doseStrengthMg;
    private String doseUnit;
    private BigDecimal serumCreatinineMgDl;
    private BigDecimal egfrMlMin;

    public enum PregnancyStatus {
        UNKNOWN,
        NOT_PREGNANT,
        POSSIBLE,
        PREGNANT
    }

    private PregnancyStatus pregnancyStatus;
    private Boolean lactating;
}
