package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSettlementStartRequest {

    /** Resignation / termination effective date (optional). */
    private LocalDate separationEffectiveDate;

    private String notes;
}
