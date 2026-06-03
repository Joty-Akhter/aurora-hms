package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EmergencyPurchaseLineResponse {

    private UUID id;
    private UUID drugId;
    private String drugName;
    private String batchNumber;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private String notes;
    private OffsetDateTime createdAt;
}
