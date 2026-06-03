package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ES-28–ES-30: Proration result for an employee in a pay period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrationDto {
    /** Days worked in the period (from join/relieving dates). */
    private int daysWorked;
    /** Total calendar days in the period. */
    private int totalDaysInPeriod;
    /** Join date used (employee hire or override). */
    private LocalDate joinDateUsed;
    /** Relieving date used (employee termination or override or period end). */
    private LocalDate relievingDateUsed;
    /** Prorated amount when fullAmount and component proration rule are applied (optional). */
    private BigDecimal proratedAmount;
    /** Full amount before proration (echo). */
    private BigDecimal fullAmount;
    /** Proration rule applied (BY_DAYS, NO_PRORATION, BY_HOURS). */
    private String prorationRule;
}
