package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** ES-26 / INT-01–INT-08 / INT-14–INT-18: Payslip with component order, statutory semantics, and optional YTD. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipDto {
    private UUID payrollRunId;
    private UUID payrollDetailId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private BigDecimal basicSalary;
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    /** Populated when payroll was calculated with time and attendance roll-up. */
    private Integer workingDays;
    private BigDecimal presentDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeAmount;
    private BigDecimal lopDays;
    private BigDecimal lopAmount;
    /** Currency resolved from employee's salary structure for the payroll period. */
    private String currency;
    /** Pay frequency resolved from employee's salary structure for the payroll period. */
    private String payFrequency;
    private List<PayslipLineDto> lines;

    /** INT-14 / reporting: Sum of gross pay for this employee in the calendar year of the pay period end, up to and including this run. */
    private BigDecimal yearToDateGross;
    /** Sum of total deductions (same YTD scope). */
    private BigDecimal yearToDateDeductions;
    /** Sum of net pay (same YTD scope). */
    private BigDecimal yearToDateNet;
    /** Sum of income-tax deduction lines (INCOME_TAX statutory / codes) for YTD scope. */
    private BigDecimal yearToDateIncomeTaxWithheld;
    /** INT-14: Taxable gross for this pay period (from salary component taxability/tags; recomputed on read). */
    private BigDecimal periodTaxableGross;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayslipLineDto {
        private UUID componentId;
        private String componentCode;
        private String componentName;
        private String componentType;
        private Integer displayOrder;
        private BigDecimal amount;
        /** SC-21 / INT-15: TAXABLE, EXEMPT, PARTIALLY_TAXABLE when set on salary component master. */
        private String taxability;
        /** SC-13: e.g. PF_EMPLOYEE, INCOME_TAX, ESI_EMPLOYEE. */
        private String statutoryType;
        /** SC-22: True if this earning line is tagged PF_WAGE in the component master. */
        private Boolean includedInPfWage;
        /** True if this earning line is tagged ESI_WAGE in the component master. */
        private Boolean includedInEsiWage;
        /** Legacy flag from master; combined with taxability for reporting. */
        private Boolean isTaxable;
    }
}
