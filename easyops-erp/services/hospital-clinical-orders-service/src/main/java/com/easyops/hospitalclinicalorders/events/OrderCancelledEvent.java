package com.easyops.hospitalclinicalorders.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderCancelledEvent {
    private final UUID orderSetId;
    private final UUID orderId;
    private final UUID patientId;
    private final UUID visitId;
    private final OffsetDateTime timestamp;

    public OrderCancelledEvent(UUID orderSetId, UUID orderId, UUID patientId, UUID visitId, OffsetDateTime timestamp) {
        this.orderSetId = orderSetId;
        this.orderId = orderId;
        this.patientId = patientId;
        this.visitId = visitId;
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }

    public UUID getOrderSetId() { return orderSetId; }
    public UUID getOrderId() { return orderId; }
    public UUID getPatientId() { return patientId; }
    public UUID getVisitId() { return visitId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
