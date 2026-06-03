package com.easyops.hospital.integration.card;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Outcome of attempting to issue or resolve a patient identity card via Hospital Card Service.
 */
@Value
@Builder
public class PatientIdentityCardIssuanceResult {
    String status;
    /** ISSUED, SKIPPED, FAILED, DISABLED */
    UUID cardId;
    String cardNumber;
    String message;
}
