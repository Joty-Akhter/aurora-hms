package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Internal integration: pharmacy dispense-time screening (WS-I) — delegates to prescription interaction + allergy checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseClinicalScreenRequest {

    @NotNull
    private UUID patientId;

    /** RxNorm, NDC, or other code when known; optional for catalog-only pharmacy drugs. */
    private String medicationCode;

    /** Display name for screening (e.g. generic + brand). */
    private String medicationName;
}
