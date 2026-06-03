package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Report: total discount amount by scheme in a date range.
 */
public class DiscountSummaryResponse {

    private String from;
    private String to;
    /** When schemeId was requested: single summary. */
    private DiscountSummaryItem single;
    /** When no schemeId: one item per scheme. */
    private List<DiscountSummaryItem> byScheme;

    public static class DiscountSummaryItem {
        private UUID schemeId;
        private String schemeCode;
        private BigDecimal totalAmount;
        private long decisionCount;

        public UUID getSchemeId() { return schemeId; }
        public void setSchemeId(UUID schemeId) { this.schemeId = schemeId; }
        public String getSchemeCode() { return schemeCode; }
        public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public long getDecisionCount() { return decisionCount; }
        public void setDecisionCount(long decisionCount) { this.decisionCount = decisionCount; }
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public DiscountSummaryItem getSingle() { return single; }
    public void setSingle(DiscountSummaryItem single) { this.single = single; }
    public List<DiscountSummaryItem> getByScheme() { return byScheme; }
    public void setByScheme(List<DiscountSummaryItem> byScheme) { this.byScheme = byScheme; }
}
