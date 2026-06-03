package com.easyops.hospitalpharmacy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferMovementResponse {

    private UUID movementId;
    private UUID pharmacyLocationId;
    private String pharmacyLocationName;
    private UUID drugId;
    private String genericName;
    private String brandName;
    private String movementType;
    private BigDecimal quantity;
    private String batchNumber;
    private OffsetDateTime movementTime;
    private String notes;
}

