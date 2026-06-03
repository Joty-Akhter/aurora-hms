package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class UtilizationReportResponse {

    private UUID resourceId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String groupBy; // DAY, WEEK
    private List<UtilizationDataPoint> dataPoints;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public List<UtilizationDataPoint> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<UtilizationDataPoint> dataPoints) { this.dataPoints = dataPoints; }

    public static class UtilizationDataPoint {
        private UUID resourceId;
        private LocalDate date;
        private long slotUsed;
        private Long slotAvailable;

        public UUID getResourceId() { return resourceId; }
        public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public long getSlotUsed() { return slotUsed; }
        public void setSlotUsed(long slotUsed) { this.slotUsed = slotUsed; }
        public Long getSlotAvailable() { return slotAvailable; }
        public void setSlotAvailable(Long slotAvailable) { this.slotAvailable = slotAvailable; }
    }
}
