package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Phase P4 — WS-L2 sales-style aggregate from issue movements (amounts tie to billing when integrated).
 */
@Data
@Builder
public class SalesSummaryResponse {

    private List<ConsumptionReportItemResponse> byDrug;
    private BigDecimal totalQuantityIssued;
    private int distinctDrugCount;
    /** Sum of {@code estimatedRevenue} per drug when default unit price is configured (not from billing ledger). */
    private BigDecimal estimatedRevenueTotal;
    /** Unit price used for {@code estimatedRevenueTotal}; null when no estimate is applied. */
    private BigDecimal revenueEstimateUnitPrice;
}
