package com.easyops.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSettlementShortfallRequest {

    /** Remaining debt recognized after exhausting PF / salary / other dues (ST-04). Must not exceed outstanding. */
    @NotNull
    private BigDecimal shortfallAmount;

    private String notes;
}
