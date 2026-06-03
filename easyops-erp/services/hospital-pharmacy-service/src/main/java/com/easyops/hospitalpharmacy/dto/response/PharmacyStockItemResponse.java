package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PharmacyStockItemResponse {

    private UUID stockId;
    private UUID drugId;
    private String genericName;
    private String brandName;
    private String strength;
    private String form;
    private String route;
    private String batchNumber;
    private LocalDate expiryDate;
    private BigDecimal quantityOnHand;
}

