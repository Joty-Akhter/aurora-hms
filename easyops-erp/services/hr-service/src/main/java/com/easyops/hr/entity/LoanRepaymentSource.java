package com.easyops.hr.entity;

public enum LoanRepaymentSource {
    MANUAL,
    PAYROLL,
    /** RP-05: controlled reversal of a PAYROLL posting (amount typically negative on the row). */
    PAYROLL_REVERSAL,
    /** Phase 5 exit settlement (BR-09, ST-02–ST-05). */
    PF_SETTLEMENT,
    FINAL_SALARY,
    OTHER_DUES
}
