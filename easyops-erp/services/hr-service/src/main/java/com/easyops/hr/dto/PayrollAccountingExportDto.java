package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * INT-19–INT-23: Payload exposing payroll results for accounting integration.
 * Includes both detail (per employee/component) and summary (per component) views.
 * INT-21: Consumers may post a <strong>summary</strong> journal (e.g. one expense, one deductions liability, one bank)
 * or build <strong>detail</strong> lines from {@link #detailLines}; reconciliation uses the same component codes as payslip lines.
 * INT-20: Optional {@link PayrollAccountingLineDto#getExpenseAccountCode()} / {@link PayrollAccountingLineDto#getLiabilityAccountCode()}
 * come from organization-defined salary component master when configured; otherwise finance may map by component code/category in accounting.
 * INT-41/INT-42: {@link #idempotencyKey} and {@link #correlationId} support idempotent journal posts and audit traceability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAccountingExportDto {

    private UUID organizationId;

    private UUID payrollRunId;

    /**
     * INT-42: Correlation id for this export (from {@code X-Correlation-Id} on the API request when applicable).
     */
    private String correlationId;

    /**
     * INT-41: Stable key for idempotent posting of this run’s payroll journal (same value as accounting {@code referenceId} prefix pattern).
     */
    private String idempotencyKey;

    private LocalDate payPeriodStart;

    private LocalDate payPeriodEnd;

    /** Total gross, deductions, and net for the run (from PayrollRun). */
    private java.math.BigDecimal totalGross;

    private java.math.BigDecimal totalDeductions;

    private java.math.BigDecimal totalNet;

    /** Detail lines: per employee + component (for reconciliation and audit trail). */
    private List<PayrollAccountingLineDto> detailLines;

    /** Summary lines: aggregated by component (code/type/category). */
    private List<PayrollAccountingLineDto> summaryByComponent;
}

