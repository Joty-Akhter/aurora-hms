package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** RP-01: mark an installment as administratively skipped (waived for collection) with reason. */
@Data
public class LoanInstallmentSkipRequest {

    @NotBlank
    private String reason;
}
