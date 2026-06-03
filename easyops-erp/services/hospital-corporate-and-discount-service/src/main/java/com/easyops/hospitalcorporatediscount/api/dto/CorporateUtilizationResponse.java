package com.easyops.hospitalcorporatediscount.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Report: count of discount decisions (and optionally evaluations) per corporate in a date range.
 */
public class CorporateUtilizationResponse {

    /** Date range start (inclusive), ISO-8601 date. */
    private String from;
    /** Date range end (inclusive), ISO-8601 date. */
    private String to;
    /** When corporateId was requested: single utilization. */
    private CorporateUtilizationItem single;
    /** When no corporateId: utilization per corporate. */
    private List<CorporateUtilizationItem> byCorporate;

    public static class CorporateUtilizationItem {
        private UUID corporateId;
        private long decisionCount;

        public UUID getCorporateId() { return corporateId; }
        public void setCorporateId(UUID corporateId) { this.corporateId = corporateId; }
        public long getDecisionCount() { return decisionCount; }
        public void setDecisionCount(long decisionCount) { this.decisionCount = decisionCount; }
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public CorporateUtilizationItem getSingle() { return single; }
    public void setSingle(CorporateUtilizationItem single) { this.single = single; }
    public List<CorporateUtilizationItem> getByCorporate() { return byCorporate; }
    public void setByCorporate(List<CorporateUtilizationItem> byCorporate) { this.byCorporate = byCorporate; }
}
