package com.easyops.hr.entity;

/**
 * SC-09: Calculation basis for salary components.
 * Fixed (default amount), PercentageOfBasic/PercentageOfGross, Formula, Statutory, Manual.
 */
public enum CalculationBasis {
    FIXED,
    PERCENTAGE_OF_BASIC,
    PERCENTAGE_OF_GROSS,
    FORMULA,
    STATUTORY,
    MANUAL
}
