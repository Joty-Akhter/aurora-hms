package com.easyops.hr.entity;

/**
 * Loan category classification (LC-01, LC-03).
 */
public enum LoanCategoryType {
    /** Standard term loan (Emergency, Staff, Motorcycle, etc.). */
    TERM_LOAN,
    /** Salary advance facility, reported separately from term loans. */
    SALARY_ADVANCE
}
