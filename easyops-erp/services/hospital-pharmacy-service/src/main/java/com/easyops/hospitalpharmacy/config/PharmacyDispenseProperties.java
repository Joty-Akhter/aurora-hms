package com.easyops.hospitalpharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dispense policy flags (Phase P1 — {@code pharmacy-gaps-implementation-plan.md} D1).
 * Defaults preserve legacy strict stock behavior when features are off.
 */
@Data
@ConfigurationProperties(prefix = "hospital.pharmacy.dispense")
public class PharmacyDispenseProperties {

    /**
     * When true, pharmacists may complete a line with {@link com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest#stockOverrideReason}
     * even if recorded on-hand would go negative or no stock row exists (ledger catch-up later).
     */
    private boolean allowNegativeStock = false;

    /**
     * When true, stock override requires a supervisor approval user id on the request (future).
     */
    private boolean requireSupervisorForOverride = false;

    /**
     * Phase P4 (WS-H): when true, dispensing a {@link com.easyops.hospitalpharmacy.entity.Drug#isControlledSubstance()
     * controlled} drug requires {@link com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest#getWitnessUserId()}.
     */
    private boolean requireWitnessForControlled = false;

    /**
     * Delete stored idempotency replay rows older than this many days (plan K2 TTL). Set to 0 to disable cleanup.
     */
    private int idempotencyRetentionDays = 90;
}
