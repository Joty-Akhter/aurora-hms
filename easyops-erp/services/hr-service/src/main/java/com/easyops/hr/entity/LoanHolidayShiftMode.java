package com.easyops.hr.entity;

/**
 * AD-03: how to move an installment due date when it falls on a weekend or org holiday.
 */
public enum LoanHolidayShiftMode {
    NEXT_BUSINESS_DAY,
    PREVIOUS_BUSINESS_DAY
}
