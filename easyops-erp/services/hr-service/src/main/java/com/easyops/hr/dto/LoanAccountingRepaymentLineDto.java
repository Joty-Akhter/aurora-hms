package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanRepaymentSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * PI-05 / Phase 7: one repayment transaction line for accounting export.
 */
@Value
@Builder
public class LoanAccountingRepaymentLineDto {
    UUID transactionId;
    UUID loanId;
    UUID employeeId;
    String employeeNumber;
    String employeeName;
    UUID categoryId;
    String categoryName;
    LocalDate paymentDate;
    BigDecimal amount;
    String currency;
    LoanRepaymentSource source;
    UUID payrollRunId;
    String notes;
    String journalMemo;
    String suggestedDebitAccount;
    String suggestedCreditAccount;
    /** PI-05: optional org COA codes when mapping defines LOAN_REPAYMENT. */
    String coaDebitCode;
    String coaCreditCode;
}
