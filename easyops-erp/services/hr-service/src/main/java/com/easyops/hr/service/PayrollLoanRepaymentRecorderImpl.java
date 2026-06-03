package com.easyops.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayrollLoanRepaymentRecorderImpl implements PayrollLoanRepaymentRecorder {

    private final EmployeeLoanService employeeLoanService;

    @Override
    public void recordPayrollRepayment(
            UUID organizationId,
            UUID loanId,
            BigDecimal amount,
            LocalDate paymentDate,
            UUID payrollRunId,
            UUID actorUserId) {
        employeeLoanService.recordPayrollRepayment(organizationId, loanId, amount, paymentDate, payrollRunId, actorUserId);
    }
}
