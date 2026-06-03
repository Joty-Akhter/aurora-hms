package com.easyops.hospitalpharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Phase P3 — regional walk-in / paper Rx policy ({@code pharmacy-gaps-implementation-plan.md} WS-E).
 */
@Data
@ConfigurationProperties(prefix = "hospital.pharmacy.regional")
public class PharmacyRegionalProperties {

    /**
     * When true, controlled (Rx) drugs require either a linked EHR prescription id on the order or paper/attachment evidence.
     */
    private boolean requireEhrPrescriptionForRxSkus = false;

    /**
     * When true, {@code FAILED_SOFT} external validation blocks completing lines or completing the order.
     */
    private boolean blockOnSoftValidationFailure = false;
}
