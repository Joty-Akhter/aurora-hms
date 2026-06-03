package com.easyops.hr.entity;

/**
 * LC-04: category-level interest model. Disbursement builds installments via {@code LoanScheduleBuilder}
 * ({@link #NONE} equal principal; {@link #FLAT} flat interest on principal; {@link #REDUCING_BALANCE} EMI).
 */
public enum LoanInterestMethod {
    NONE,
    FLAT,
    REDUCING_BALANCE
}
