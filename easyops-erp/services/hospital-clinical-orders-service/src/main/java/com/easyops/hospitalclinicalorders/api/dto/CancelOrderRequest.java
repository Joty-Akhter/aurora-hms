package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class CancelOrderRequest {

    @NotBlank
    private String reason;

    private UUID cancelledBy;

    /**
     * When true, allows cancellation of orders that are already COMPLETED or have FINAL results.
     * Caller must ensure only privileged users set this flag.
     */
    private Boolean adminOverride;

    /**
     * Optional explanation for admin override when cancelling completed/finalized orders.
     */
    private String overrideReason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; }
    public Boolean getAdminOverride() { return adminOverride; }
    public void setAdminOverride(Boolean adminOverride) { this.adminOverride = adminOverride; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}

