package com.easyops.hr.entity;

/**
 * SC-02: Category (sub-type) for salary components.
 * Earnings: Basic, HRA, Special Allowance, Conveyance, Medical, Leave Travel, Other Allowance.
 * Deductions: Statutory Deduction (PF, ESI, Tax), Voluntary Deduction, Loan Repayment, Recovery, Other Deduction.
 */
public enum SalaryComponentCategory {
    // Earnings
    BASIC,
    HRA,
    SPECIAL_ALLOWANCE,
    CONVEYANCE,
    MEDICAL,
    LEAVE_TRAVEL,
    OTHER_ALLOWANCE,
    // Deductions
    STATUTORY_DEDUCTION,
    VOLUNTARY_DEDUCTION,
    LOAN_REPAYMENT,
    RECOVERY,
    OTHER_DEDUCTION
}
