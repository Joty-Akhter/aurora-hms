package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** RP-05: reverse a posted PAYROLL repayment with audit trail. */
@Data
public class LoanPayrollRepaymentReversalRequest {

    @NotBlank
    private String reason;
}
