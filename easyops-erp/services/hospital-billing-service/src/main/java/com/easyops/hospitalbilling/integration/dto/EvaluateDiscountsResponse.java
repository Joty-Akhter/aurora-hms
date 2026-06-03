package com.easyops.hospitalbilling.integration.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response from the corporate/discount service with per-line discount amounts and sources.
 */
public class EvaluateDiscountsResponse {

    private List<LineDiscountResult> lineDiscounts = new ArrayList<>();
    private BigDecimal recommendedTotalDiscount;

    public List<LineDiscountResult> getLineDiscounts() {
        return lineDiscounts;
    }

    public void setLineDiscounts(List<LineDiscountResult> lineDiscounts) {
        this.lineDiscounts = lineDiscounts != null ? lineDiscounts : new ArrayList<>();
    }

    public BigDecimal getRecommendedTotalDiscount() {
        return recommendedTotalDiscount;
    }

    public void setRecommendedTotalDiscount(BigDecimal recommendedTotalDiscount) {
        this.recommendedTotalDiscount = recommendedTotalDiscount;
    }
}
