package com.easyops.hr.entity;

public enum EmployeeLoanStatus {
    PENDING_DISBURSEMENT,
    ACTIVE,
    /** Exit / resignation settlement in progress (ST-01). */
    SETTLEMENT_PENDING,
    CLOSED
}
