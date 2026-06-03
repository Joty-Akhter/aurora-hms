package com.easyops.hospitalcard.domain.limit;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result of a limit check before authorization.
 */
public class LimitCheckResult {

    private final boolean allowed;
    private final String reasonCode;
    private final Map<String, Object> remainingLimits;

    public LimitCheckResult(boolean allowed, String reasonCode, Map<String, Object> remainingLimits) {
        this.allowed = allowed;
        this.reasonCode = reasonCode;
        this.remainingLimits = remainingLimits;
    }

    public static LimitCheckResult allowed() {
        return new LimitCheckResult(true, null, null);
    }

    public static LimitCheckResult limitExceeded(String reasonCode, Map<String, Object> remainingLimits) {
        return new LimitCheckResult(false, reasonCode, remainingLimits);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Map<String, Object> getRemainingLimits() {
        return remainingLimits;
    }
}
