package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanRepaymentSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSettlementAllocateRequest {

    /** Must be PF_SETTLEMENT, FINAL_SALARY, or OTHER_DUES. */
    @NotNull
    private LoanRepaymentSource source;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDate paymentDate;

    /** External reference (PF batch, payroll id, etc.). */
    private String reference;

    private String notes;
}
