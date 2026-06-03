package com.easyops.hr.entity;

public enum LoanInstallmentStatus {
    DUE,
    PARTIAL,
    PAID,
    /** RP-01: installment waived for this period; see skip_reason on the installment row. */
    SKIPPED
}
