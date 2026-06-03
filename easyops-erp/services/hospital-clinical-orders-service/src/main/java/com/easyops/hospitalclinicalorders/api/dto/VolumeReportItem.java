package com.easyops.hospitalclinicalorders.api.dto;

/**
 * One row of volume report: count of orders in a period, grouped by orderType or department.
 * groupKey is order_type string when groupBy=orderType, or ordering_department_id (UUID string) when groupBy=department.
 */
public class VolumeReportItem {

    private String groupKey;
    private long count;

    public VolumeReportItem() {}

    public VolumeReportItem(String groupKey, long count) {
        this.groupKey = groupKey;
        this.count = count;
    }

    public String getGroupKey() { return groupKey; }
    public void setGroupKey(String groupKey) { this.groupKey = groupKey; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
