package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanReportSummaryDto {

    private long activeLoanCount;
    private long pendingDisbursementCount;
    private long settlementPendingCount;
    private long closedLoanCount;
    private BigDecimal totalOutstanding;
    private BigDecimal totalArrearsRemaining;
    private long arrearsInstallmentCount;
}
