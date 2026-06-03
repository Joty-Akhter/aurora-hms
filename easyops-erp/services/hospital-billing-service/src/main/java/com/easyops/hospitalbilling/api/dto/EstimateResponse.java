package com.easyops.hospitalbilling.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for an estimate: per-line amounts and totals with discount applied; net payable.
 */
public class EstimateResponse {

    private List<EstimateLineResponse> lines = new ArrayList<>();
    private List<DiscountLineResponse> discountLines = new ArrayList<>();
    private BigDecimal totalGross = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal netPayable = BigDecimal.ZERO;

    public List<EstimateLineResponse> getLines() {
        return lines;
    }

    public void setLines(List<EstimateLineResponse> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public List<DiscountLineResponse> getDiscountLines() {
        return discountLines;
    }

    public void setDiscountLines(List<DiscountLineResponse> discountLines) {
        this.discountLines = discountLines != null ? discountLines : new ArrayList<>();
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = totalGross != null ? totalGross : BigDecimal.ZERO;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount != null ? totalDiscount : BigDecimal.ZERO;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable != null ? netPayable : BigDecimal.ZERO;
    }
}
