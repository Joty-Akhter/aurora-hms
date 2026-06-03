package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanHolidayShiftMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOrganizationSettingsDto {

    private UUID organizationId;
    private Integer minTenureMonths;
    private BigDecimal maxPrincipalAmount;
    private String currency;
    private Boolean enforceSingleActiveLoan;
    private Boolean allowSalaryAdvanceWithActiveTermLoan;
    /** EL-02: employment_status values that block loan applications (case-insensitive). */
    private List<String> disqualifyingEmploymentStatuses;
    /** ST-03: ordered source names matching LoanRepaymentSource for exit settlement. */
    private List<String> settlementAllocationPriority;
    private Boolean enforceSettlementAllocationOrder;
    /** AL-03: when true, HR approval alone creates the loan (no Finance step). */
    private Boolean skipFinanceApproval;
    /** BR-08: when true, salary advance skips Finance (HR only). */
    private Boolean salaryAdvanceSkipFinanceApproval;
    /** AD-03: shift new installment due dates off weekends and org holidays. */
    private Boolean shiftInstallmentDueDatesForHolidays;
    private LoanHolidayShiftMode loanHolidayShiftMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
