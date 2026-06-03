package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * INT-01–INT-08: Summary of populating a payroll run from salary management.
 * Contains counts and error information (employees without assignment / missing Basic).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPopulationSummaryDto {

    private UUID payrollRunId;

    /**
     * Number of employees for whom payroll details/components were successfully created.
     */
    private int employeesPopulated;

    /**
     * Employees who were skipped because no active salary assignment existed as of the payroll period end.
     */
    private List<UUID> employeesWithoutAssignment;

    /**
     * Employees whose salary data did not contain a Basic earning component when computing payroll.
     * Payroll details are still created with basicAmount = 0, but this list is returned for error handling/reporting.
     */
    private List<UUID> employeesMissingBasic;

    /**
     * INT-09 / ES-28: Human-readable notices (e.g. PF wage 0 while PF statutory lines exist; proration note).
     */
    @Builder.Default
    private List<String> warnings = List.of();
}

