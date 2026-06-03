package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PrescriptionTransmission;
import com.easyops.hospital.validation.ValidFillStatusRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidFillStatusRequest
@Schema(description = "Inbound pharmacy fill-status callback payload (FR-P3.11a)")
public class FillStatusUpdateRequest {

    @NotNull(message = "Transmission ID is required")
    @Schema(description = "Network transaction ID returned at transmission time; used to correlate to ehr.prescription_transmissions",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String networkTransactionId;

    @NotNull(message = "Fill status is required")
    @Schema(description = """
            Current fill status reported by the pharmacy.  All 10 values are accepted as inbound codes.
            PICKED_UP, CANCELLED, REJECTED, and EXPIRED are terminal states: the system will reject any
            subsequent transition away from them with HTTP 409.""",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
                "PENDING", "IN_PROGRESS", "ON_HOLD", "OUT_OF_STOCK",
                "PARTIALLY_FILLED", "FILLED", "PICKED_UP",
                "CANCELLED", "REJECTED", "EXPIRED"
            })
    private PrescriptionTransmission.FillStatus fillStatus;

    /**
     * ISO-8601 UTC timestamp of the fill event as recorded by the pharmacy system.
     * <p>
     * This field is always required. The server will never substitute
     * {@code LocalDateTime.now()} for a missing value — doing so would corrupt
     * audit and billing reconciliation with an incorrect event timestamp.
     * Requests with a null {@code fillStatusDate} are rejected with HTTP 400.
     */
    @NotNull(message = "fillStatusDate is required and must reflect the pharmacy event time; "
            + "the server does not substitute a server-generated timestamp")
    @Schema(description = """
            ISO-8601 UTC timestamp of the fill event at the pharmacy (required, always).
            The server never substitutes now() for a missing value — the timestamp must originate
            from the pharmacy system to preserve accurate fill-event timing for audit and billing.
            Null or absent fillStatusDate is rejected with HTTP 400.""",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-04-04T14:30:00Z")
    private LocalDateTime fillStatusDate;

    @Schema(description = "Human-readable status note from the pharmacy (optional)")
    private String fillStatusMessage;

    @Schema(description = "Timestamp the prescription was dispensed. Required when fillStatus is FILLED or PARTIALLY_FILLED.",
            example = "2026-04-04T14:45:00Z")
    private LocalDateTime filledDate;

    @Schema(description = "Timestamp the patient picked up the prescription. Required when fillStatus is PICKED_UP.",
            example = "2026-04-04T16:00:00Z")
    private LocalDateTime pickedUpDate;

    @Schema(description = "True when the pharmacy initiated the cancellation (as opposed to a provider or patient cancellation)")
    private Boolean cancelledByPharmacy;

    @Schema(description = "Reason for cancellation or rejection. Required when fillStatus is CANCELLED or REJECTED.")
    private String cancellationReason;

    // Network / pharmacy identity fields (informational; sourced from network callback)
    @Schema(description = "Name of the e-prescribing network or pharmacy relay (informational)")
    private String networkName;

    @Schema(description = "NPI of the dispensing pharmacy")
    private String pharmacyNpi;

    @Schema(description = "Name of the dispensing pharmacy")
    private String pharmacyName;
}
