package com.easyops.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Full or partial disbursement for loans in {@code PENDING_DISBURSEMENT} (AL-05).
 */
@Data
public class LoanDisbursementRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private LocalDate disbursementDate;
}
