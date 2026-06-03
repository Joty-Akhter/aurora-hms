package com.easyops.hospitalpharmacy.dto.response;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Phase P4 — controlled substance register row (WS-H3).
 */
@Data
@Builder
public class ControlledSubstanceDispenseRowResponse {

    private UUID dispenseLineId;
    private UUID dispenseOrderId;
    private UUID patientId;
    private UUID pharmacyLocationId;
    private String pharmacyLocationName;
    private UUID drugId;
    private String genericName;
    private String brandName;
    private String controlledProfileCode;
    private BigDecimal quantityDispensed;
    private String batchNumber;
    private DispenseLine.Status lineStatus;
    private OffsetDateTime dispensedAt;
    private UUID witnessUserId;
}
