package com.easyops.hospitalpharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Phase P2 — billing posting, EHR in-house fill sync, and outgoing domain events.
 */
@Data
@ConfigurationProperties(prefix = "hospital.pharmacy.integration")
public class PharmacyIntegrationProperties {

    private Billing billing = new Billing();
    private HospitalService hospitalService = new HospitalService();
    private Events events = new Events();
    /** Phase P4 WS-I — hospital-service clinical checks at dispense (optional). */
    private ClinicalSafety clinicalSafety = new ClinicalSafety();

    @Data
    public static class Billing {
        /** When true, completing a dispense order posts charges to hospital-billing-service. */
        private boolean postChargesEnabled = false;
        private BigDecimal defaultUnitPrice = BigDecimal.ZERO;
        private String sourceServiceName = "hospital-pharmacy-service";
        /** Credit lines when returns are recorded (negative quantity charges). */
        private boolean postReturnCreditsEnabled = false;
    }

    @Data
    public static class HospitalService {
        private boolean inHouseFillEnabled = true;
    }

    @Data
    public static class Events {
        private boolean publishEnabled = true;
        private String topic = "hospital-events";
        /**
         * When set, {@code prescription.created} Kafka handling creates the dispense queue order at this
         * pharmacy instead of the first active site (Phase P2 — WS-K3).
         */
        private UUID defaultPharmacyLocationId;
    }

    @Data
    public static class ClinicalSafety {
        /**
         * When true, each dispense line calls hospital-service interaction + allergy screening before stock moves.
         */
        private boolean checkAtDispenseEnabled = false;
        /**
         * Interaction severities that block unless {@code clinicalSafetyOverrideReason} is sent on the line (I2).
         */
        private List<String> blockInteractionSeverities = List.of("CONTRAINDICATED", "MAJOR");
        /**
         * When false, allergy rows with matchType {@code CROSS_REACTIVITY} do not block (soft alert only).
         */
        private boolean blockOnCrossReactivityAllergies = false;
    }
}
