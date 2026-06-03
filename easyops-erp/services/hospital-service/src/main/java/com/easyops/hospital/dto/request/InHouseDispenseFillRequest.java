package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PrescriptionTransmission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * In-house hospital pharmacy dispense completion → EHR prescription / transmission sync (Phase P2 — WS-B).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Apply fill status from internal dispensing (not the external network webhook)")
public class InHouseDispenseFillRequest {

    @NotNull
    private UUID prescriptionId;

    @NotNull
    private UUID dispenseOrderId;

    @NotBlank
    @Schema(description = "Idempotency key for retries (e.g. inhouse-fill-{dispenseOrderId})")
    private String idempotencyKey;

    @NotNull
    private PrescriptionTransmission.FillStatus fillStatus;

    /**
     * When the pharmacy recorded this fill (required for audit; same semantics as fill-status webhook).
     */
    @NotNull
    private LocalDateTime fillStatusDate;

    @Schema(description = "Required when fillStatus is FILLED or PARTIALLY_FILLED")
    private LocalDateTime filledDate;

    @Schema(description = "Optional note stored on transmission / history")
    private String fillStatusMessage;

    @Schema(description = "Per-medication line quantities for audit (optional)")
    private List<InHouseDispenseFillLineRequest> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InHouseDispenseFillLineRequest {
        private UUID prescriptionMedicationId;
        private java.math.BigDecimal quantityDispensed;
    }
}
