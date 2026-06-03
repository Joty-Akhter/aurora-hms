package com.easyops.hr.entity;

/**
 * Loan application workflow (Phase 2).
 */
public enum LoanApplicationStatus {
    DRAFT,
    /** Submitted; awaiting HR approval (AL-03). */
    SUBMITTED,
    /** HR approved; awaiting Finance approval (AL-03). */
    PENDING_FINANCE_APPROVAL,
    /** Applicant must update details after clarification request. */
    AWAITING_CLARIFICATION,
    APPROVED,
    REJECTED,
    CANCELLED
}
