package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response for GET /reports/corporate-exposure: list of cards and summary total.
 */
public class CorporateExposureResponse {

    private List<CorporateExposureItem> items;
    private BigDecimal totalBalance;

    public List<CorporateExposureItem> getItems() {
        return items;
    }

    public void setItems(List<CorporateExposureItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
}
