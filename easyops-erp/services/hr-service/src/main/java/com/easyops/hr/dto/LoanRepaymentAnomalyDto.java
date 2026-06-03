package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** RP-05: lightweight alert row for payroll recovery review (reversals, cross-checks). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentAnomalyDto {

    public enum AnomalyType {
        PAYROLL_REVERSAL_RECORDED,
        /** Payslip loan component total differs from loan module PAYROLL posting for the run. */
        PAYSLIP_VS_LOAN_POSTING_MISMATCH,
        /** Recovery preview is positive but no PAYROLL posting for a finalized payroll run. */
        EXPECTED_RECOVERY_NOT_POSTED
    }

    private AnomalyType type;
    private UUID payrollRunId;
    private UUID loanId;
    private UUID employeeId;
    private UUID transactionId;
    private String message;
    private LocalDateTime detectedAt;
    /** Payslip-side loan repayment total (absolute) when type is PAYSLIP_VS_LOAN_POSTING_MISMATCH. */
    private BigDecimal payslipLoanAmount;
    /** Posted PAYROLL transaction amount (absolute) when applicable. */
    private BigDecimal postedLoanAmount;
    private BigDecimal varianceAmount;
}
