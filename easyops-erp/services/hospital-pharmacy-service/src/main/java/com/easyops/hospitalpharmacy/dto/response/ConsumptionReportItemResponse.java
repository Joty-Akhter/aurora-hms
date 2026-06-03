package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ConsumptionReportItemResponse {

    private UUID drugId;
    private String genericName;
    private String brandName;
    private String strength;
    private String form;
    private String route;
    private BigDecimal totalQuantityIssued;
    /** Set on sales-summary when {@code hospital.pharmacy.integration.billing.default-unit-price} &gt; 0. */
    private BigDecimal estimatedRevenue;
}

