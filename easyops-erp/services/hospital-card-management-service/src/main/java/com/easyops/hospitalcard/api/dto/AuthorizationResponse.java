package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class AuthorizationResponse {

    private boolean approved;
    private UUID authorizationId;
    /** INSUFFICIENT_BALANCE, LIMIT_EXCEEDED, CARD_BLOCKED, CARD_NOT_FOUND, etc. */
    private String reasonCode;
    private BigDecimal remainingBalance;
    /** Phase 3: remaining limits */
    private Object remainingLimits;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public UUID getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(UUID authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public Object getRemainingLimits() {
        return remainingLimits;
    }

    public void setRemainingLimits(Object remainingLimits) {
        this.remainingLimits = remainingLimits;
    }
}
