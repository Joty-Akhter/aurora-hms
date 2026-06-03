package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create a new order set by copying an existing one (repeat order / re-order).
 * New order set has same patient, visit, ordering doctor/department, and same order lines (new ids); all orders status REQUESTED.
 */
public class CopyOrderSetRequest {

    @NotNull(message = "sourceOrderSetId is required")
    private UUID sourceOrderSetId;

    /** Optional override for order context (OPD, IPD, ED). If null, copied from source. */
    private String orderContext;

    /** Optional override for priority (STAT, ROUTINE, URGENT). If null, copied from source. */
    private String priority;

    /** Optional override for facility. If null, copied from source. */
    private UUID facilityId;

    public UUID getSourceOrderSetId() { return sourceOrderSetId; }
    public void setSourceOrderSetId(UUID sourceOrderSetId) { this.sourceOrderSetId = sourceOrderSetId; }
    public String getOrderContext() { return orderContext; }
    public void setOrderContext(String orderContext) { this.orderContext = orderContext; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
}
