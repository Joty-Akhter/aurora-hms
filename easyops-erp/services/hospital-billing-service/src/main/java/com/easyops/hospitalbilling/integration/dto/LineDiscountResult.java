package com.easyops.hospitalbilling.integration.dto;

import java.math.BigDecimal;

/**
 * Per-line discount result from the corporate/discount service.
 */
public class LineDiscountResult {

    /** Zero-based index of the line in the request. */
    private int lineIndex;
    private BigDecimal discountAmount;
    /** Source/scheme code (e.g. "CORPORATE_10", "PROMO_LAB"). */
    private String source;

    public int getLineIndex() {
        return lineIndex;
    }

    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
