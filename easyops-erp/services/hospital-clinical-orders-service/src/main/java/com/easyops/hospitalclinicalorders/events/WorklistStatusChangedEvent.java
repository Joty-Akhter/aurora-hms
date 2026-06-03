package com.easyops.hospitalclinicalorders.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public class WorklistStatusChangedEvent {
    private final UUID worklistItemId;
    private final UUID orderId;
    private final String status;
    private final OffsetDateTime timestamp;

    public WorklistStatusChangedEvent(UUID worklistItemId, UUID orderId, String status, OffsetDateTime timestamp) {
        this.worklistItemId = worklistItemId;
        this.orderId = orderId;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }

    public UUID getWorklistItemId() { return worklistItemId; }
    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
