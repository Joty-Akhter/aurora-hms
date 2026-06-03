package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DispenseLineRequest {

    @NotNull
    private UUID drugId;

    private UUID prescriptionLineId;

    private String batchNumber;

    private BigDecimal quantityPrescribed;

    @NotNull
    private BigDecimal quantityDispensed;

    /**
     * Required when {@link com.easyops.hospitalpharmacy.config.PharmacyDispenseProperties#allowNegativeStock}
     * is true and recorded stock cannot satisfy the issue without going negative (or no stock row exists).
     * Persisted to {@code dispense_lines.override_reason_code}.
     */
    @Size(max = 2000)
    private String stockOverrideReason;

    /**
     * Second user witnessing controlled-substance dispense when {@code hospital.pharmacy.dispense.require-witness-for-controlled}
     * is true (Phase P4 — WS-H).
     */
    private UUID witnessUserId;

    /**
     * When substituting for a formulary-restricted drug, set to the restricted drug id; {@link #drugId} is the SKU issued.
     */
    private UUID substitutedDrugId;

    /** Required when dispensing a restricted formulary drug directly (no substitution). Phase P3 WS-G. */
    @Size(max = 2000)
    private String formularyOverrideReason;

    /** P4 WS-I — required when clinical safety screening blocks the line and policy allows documented override. */
    @Size(max = 2000)
    private String clinicalSafetyOverrideReason;
}

