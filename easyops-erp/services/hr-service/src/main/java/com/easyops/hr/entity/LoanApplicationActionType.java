package com.easyops.hr.entity;

public enum LoanApplicationActionType {
    CREATED,
    UPDATED,
    SUBMITTED,
    /** HR step completed (AL-03). */
    HR_APPROVED,
    /** Finance step completed; loan created (AL-03). */
    FINANCE_APPROVED,
    /** Legacy single-step approve — prefer HR_APPROVED + FINANCE_APPROVED. */
    APPROVED,
    CLARIFICATION_REQUESTED,
    RESUBMITTED_AFTER_CLARIFICATION,
    DELEGATED,
    REJECTED,
    CANCELLED
}
