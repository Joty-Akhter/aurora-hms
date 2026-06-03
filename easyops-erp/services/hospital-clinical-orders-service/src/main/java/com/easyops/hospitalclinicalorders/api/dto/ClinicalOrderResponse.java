package com.easyops.hospitalclinicalorders.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ClinicalOrderResponse {
    private UUID id;
    private UUID orderSetId;
    private String orderType;
    private String itemCode;
    private String status;
    private String priority;
    private String orderingNotes;
    private OffsetDateTime performedAt;
    private UUID performedBy;
    private String cancelReason;
    private OffsetDateTime cancelledAt;
    private UUID cancelledBy;
    private String externalSystemId;
    private String resultStatus;
    private OffsetDateTime resultAvailableAt;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private UUID facilityId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderSetId() { return orderSetId; }
    public void setOrderSetId(UUID orderSetId) { this.orderSetId = orderSetId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getOrderingNotes() { return orderingNotes; }
    public void setOrderingNotes(String orderingNotes) { this.orderingNotes = orderingNotes; }
    public OffsetDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(OffsetDateTime performedAt) { this.performedAt = performedAt; }
    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getExternalSystemId() { return externalSystemId; }
    public void setExternalSystemId(String externalSystemId) { this.externalSystemId = externalSystemId; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public OffsetDateTime getResultAvailableAt() { return resultAvailableAt; }
    public void setResultAvailableAt(OffsetDateTime resultAvailableAt) { this.resultAvailableAt = resultAvailableAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
}
