package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Payroll detail with employee name for display. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDetailWithEmployeeDto {
    private UUID payrollDetailId;
    private UUID payrollRunId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private BigDecimal basicSalary;
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalReimbursements;
    private BigDecimal netSalary;
    private Integer workingDays;
    private BigDecimal presentDays;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeAmount;
    private BigDecimal lopDays;
    private BigDecimal lopAmount;
    private BigDecimal bonusAmount;
    private String status;
    private String paymentReference;
    private LocalDateTime paidAt;
}
