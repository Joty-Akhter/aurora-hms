package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanHolidayShiftMode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial update: null fields are ignored.
 */
@Data
public class LoanOrganizationSettingsPatchRequest {

    private Integer minTenureMonths;
    private BigDecimal maxPrincipalAmount;
    private String currency;
    private Boolean enforceSingleActiveLoan;
    private Boolean allowSalaryAdvanceWithActiveTermLoan;
    private List<String> disqualifyingEmploymentStatuses;
    private List<String> settlementAllocationPriority;
    private Boolean enforceSettlementAllocationOrder;
    private Boolean skipFinanceApproval;
    private Boolean salaryAdvanceSkipFinanceApproval;
    private Boolean shiftInstallmentDueDatesForHolidays;
    private LoanHolidayShiftMode loanHolidayShiftMode;
}
