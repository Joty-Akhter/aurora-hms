package com.easyops.hr.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * INT-11 Option A/B: PF module output for one employee per pay period — employee/employer amounts from PF wage + policy.
 */
@Value
@Builder
public class EpfPayrollContributionResult {
    BigDecimal pfWageBeforeCeiling;
    BigDecimal pfWageAfterCeiling;
    BigDecimal employeeContributionAmount;
    BigDecimal employerContributionAmount;
    BigDecimal employerEpfAmount;
    BigDecimal employerPensionAmount;
    BigDecimal employerEdliAmount;
    BigDecimal employerAdminChargeAmount;
    BigDecimal employeeRatePercent;
    BigDecimal employerRatePercent;
    boolean eligible;
    String ineligibilityReason;

    public static EpfPayrollContributionResult ineligible(BigDecimal pfWageBefore, BigDecimal pfWageAfter,
                                                           BigDecimal empRate, BigDecimal emprRate, String reason) {
        return EpfPayrollContributionResult.builder()
                .pfWageBeforeCeiling(pfWageBefore)
                .pfWageAfterCeiling(pfWageAfter)
                .employeeContributionAmount(BigDecimal.ZERO)
                .employerContributionAmount(BigDecimal.ZERO)
                .employerEpfAmount(BigDecimal.ZERO)
                .employerPensionAmount(BigDecimal.ZERO)
                .employerEdliAmount(BigDecimal.ZERO)
                .employerAdminChargeAmount(BigDecimal.ZERO)
                .employeeRatePercent(empRate)
                .employerRatePercent(emprRate)
                .eligible(false)
                .ineligibilityReason(reason)
                .build();
    }
}
