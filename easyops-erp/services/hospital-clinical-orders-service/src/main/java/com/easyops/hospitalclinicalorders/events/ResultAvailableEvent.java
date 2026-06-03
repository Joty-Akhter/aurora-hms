package com.easyops.hospitalclinicalorders.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload aligned with §6: orderSetId, orderId, patientId, visitId, status, timestamp, source.
 */
public class ResultAvailableEvent {
    private final UUID orderId;
    private final UUID orderSetId;
    private final UUID patientId;
    private final UUID visitId;
    private final UUID resultLinkId;
    private final String resultStatus;
    private final String source;
    private final OffsetDateTime timestamp;

    public ResultAvailableEvent(UUID orderId, UUID orderSetId, UUID patientId, UUID visitId,
                                UUID resultLinkId, String resultStatus, String source, OffsetDateTime timestamp) {
        this.orderId = orderId;
        this.orderSetId = orderSetId;
        this.patientId = patientId;
        this.visitId = visitId;
        this.resultLinkId = resultLinkId;
        this.resultStatus = resultStatus != null ? resultStatus : "";
        this.source = source != null ? source : "";
        this.timestamp = timestamp != null ? timestamp : OffsetDateTime.now();
    }

    public UUID getOrderId() { return orderId; }
    public UUID getOrderSetId() { return orderSetId; }
    public UUID getPatientId() { return patientId; }
    public UUID getVisitId() { return visitId; }
    public UUID getResultLinkId() { return resultLinkId; }
    public String getResultStatus() { return resultStatus; }
    public String getSource() { return source; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
