package com.easyops.hr.dto;

import java.math.BigDecimal;

/**
 * Aggregated time and attendance for a single employee over a pay period, used to populate
 * payroll detail rows and derive LOP and overtime amounts.
 */
public record PayrollAttendanceRollup(
        int workingDays,
        BigDecimal presentDays,
        BigDecimal leaveDays,
        BigDecimal lopDays,
        BigDecimal overtimeHours,
        boolean hasAttendanceOrTimesheetData
) {
    public static PayrollAttendanceRollup empty(int workingDays) {
        return new PayrollAttendanceRollup(workingDays, null, null, null, null, false);
    }
}
