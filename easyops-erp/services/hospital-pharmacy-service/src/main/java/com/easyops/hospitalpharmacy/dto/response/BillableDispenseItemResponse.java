package com.easyops.hospitalpharmacy.dto.response;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only billable view for {@code GET .../dispense-orders/{id}/billable-items} (Phase P1 — plan A1).
 * Final pricing remains with {@code hospital-billing-service}; optional list price for display only.
 */
@Data
@Builder
public class BillableDispenseItemResponse {

    private UUID dispenseLineId;
    private UUID dispenseOrderId;
    private UUID drugId;
    private String drugGenericName;
    private String drugBrandName;
    private String strength;
    private String form;
    private String unitOfMeasure;
    private String batchNumber;
    private BigDecimal quantityPrescribed;
    private BigDecimal quantityDispensed;
    private DispenseLine.Status lineStatus;
    /** Free-text stock override when {@code lineStatus} is {@link DispenseLine.Status#FILLED_WITH_STOCK_OVERRIDE}; null otherwise. */
    private String overrideReasonCode;
    /** Placeholder until drug master carries MRP; null when unknown. */
    private BigDecimal suggestedListPrice;
    private String taxCodeHint;
}
