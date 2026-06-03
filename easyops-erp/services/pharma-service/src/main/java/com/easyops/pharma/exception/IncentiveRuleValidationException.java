package com.easyops.pharma.exception;

/**
 * Thrown when incentive rule validation fails (allocation sum, dual-role, employees in territory).
 */
public class IncentiveRuleValidationException extends RuntimeException {

    public IncentiveRuleValidationException(String message) {
        super(message);
    }

    public IncentiveRuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
