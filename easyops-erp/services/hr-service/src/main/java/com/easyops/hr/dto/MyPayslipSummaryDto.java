package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** ES-54: Summary of a payroll run for self-service "my payslips" list (view/download payslip). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPayslipSummaryDto {
    private UUID payrollRunId;
    private String runName;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate paymentDate;
    private String status;
}
