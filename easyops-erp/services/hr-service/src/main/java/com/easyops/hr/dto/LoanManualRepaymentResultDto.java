package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanManualRepaymentResultDto {

    private UUID transactionId;
    private UUID loanId;
    private BigDecimal totalAmount;
    private BigDecimal newOutstandingBalance;
    private boolean loanClosed;
    private List<LoanRepaymentAllocationDto> allocations;
}
