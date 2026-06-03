package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * INT-19–INT-23: One payroll result line for accounting export.
 * Can be used for either detail (per employee/component) or summary (per component).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAccountingLineDto {

    /** Optional: employee for detail-level rows; null for pure summary rows. */
    private UUID employeeId;

    private String employeeNumber;

    private UUID componentId;

    private String componentCode;

    private String componentType; // EARNING / DEDUCTION

    /** Optional: category such as BASIC, STATUTORY_DEDUCTION, etc. */
    private String category;

    /** Amount for this row (positive numbers only). */
    private BigDecimal amount;

    /** INT-20: From salary component master when set (earning → expense recognition). */
    private String expenseAccountCode;

    /** INT-20: From salary component master when set (deduction → liability/withholding). */
    private String liabilityAccountCode;
}

