package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public class CancellationReportResponse {

    private UUID resourceId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long count;
    private Long totalAppointmentsInRange;
    private Double cancellationRate;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public Long getTotalAppointmentsInRange() { return totalAppointmentsInRange; }
    public void setTotalAppointmentsInRange(Long totalAppointmentsInRange) { this.totalAppointmentsInRange = totalAppointmentsInRange; }
    public Double getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(Double cancellationRate) { this.cancellationRate = cancellationRate; }
}
