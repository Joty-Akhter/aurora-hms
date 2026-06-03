package com.easyops.hospitalclinicalorders.api.dto;

/**
 * One row of TAT report: aggregate turnaround time (order created to result_available_at or completed) by order type.
 */
public class TatReportItem {

    private String orderType;
    private long count;
    private Double avgTatHours;

    public TatReportItem() {}

    public TatReportItem(String orderType, long count, Double avgTatHours) {
        this.orderType = orderType;
        this.count = count;
        this.avgTatHours = avgTatHours;
    }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public Double getAvgTatHours() { return avgTatHours; }
    public void setAvgTatHours(Double avgTatHours) { this.avgTatHours = avgTatHours; }
}
