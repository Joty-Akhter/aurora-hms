package com.easyops.hr.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * PI-05 / Phase 7: one disbursement line for accounting export (journal suggestion + reconciliation).
 */
@Value
@Builder
public class LoanAccountingDisbursementLineDto {
    UUID loanId;
    UUID employeeId;
    String employeeNumber;
    String employeeName;
    UUID categoryId;
    String categoryName;
    LocalDate disbursementDate;
    BigDecimal amount;
    String currency;
    /** Human-readable line description for GL memo / narration. */
    String journalMemo;
    /** Suggested debit account label (map to COA in accounting module). */
    String suggestedDebitAccount;
    /** Suggested credit account label. */
    String suggestedCreditAccount;
    /** PI-05: optional org COA codes when {@code loan_accounting_coa_mappings} defines LOAN_DISBURSEMENT. */
    String coaDebitCode;
    String coaCreditCode;
}
