package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRecoveryLineDto {

    private UUID loanId;
    private UUID employeeId;
    private BigDecimal amount;
    private String currency;
    /** True when payrollRunId was supplied and a PAYROLL posting already exists for this loan+run. */
    private boolean alreadyPostedForRun;
}
