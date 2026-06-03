package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StockRequisitionLineResponse {

    private UUID id;
    private UUID drugId;
    private String genericName;
    private String brandName;
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;
    private BigDecimal receivedQuantity;
    private String batchNumber;
    private String notes;
}
