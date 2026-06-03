package com.easyops.hr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Narrow port for posting payroll loan deductions (Phase 4). Implemented by delegation to {@link EmployeeLoanService}.
 */
public interface PayrollLoanRepaymentRecorder {

    void recordPayrollRepayment(
            UUID organizationId,
            UUID loanId,
            BigDecimal amount,
            LocalDate paymentDate,
            UUID payrollRunId,
            UUID actorUserId);
}
