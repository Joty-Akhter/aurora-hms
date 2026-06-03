package com.easyops.hr.entity;

/**
 * ST-04: coarse legal / write-off workflow — not a full task engine; labels are org-policy driven.
 */
public enum LoanLegalWorkflowStatus {
    PENDING_LEGAL,
    PENDING_BOARD,
    RESOLVED,
    CLOSED
}
