package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Phase P4 — WS-L1 stock override audit lines. */
@Data
@Builder
public class StockOverrideLineReportResponse {

    private UUID dispenseLineId;
    private UUID dispenseOrderId;
    private UUID pharmacyLocationId;
    private String pharmacyLocationName;
    private UUID drugId;
    private String genericName;
    private String overrideReasonCode;
    private BigDecimal quantityDispensed;
    private OffsetDateTime dispensedAt;
}
