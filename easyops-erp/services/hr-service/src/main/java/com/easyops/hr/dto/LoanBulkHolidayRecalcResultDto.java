package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** AD-03: summary of org-wide holiday due-date recalculation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanBulkHolidayRecalcResultDto {

    private UUID organizationId;
    private int loansRecalculated;
    private int loansSkipped;
    @Builder.Default
    private List<LoanBulkHolidayRecalcFailureDto> failures = new ArrayList<>();
}
