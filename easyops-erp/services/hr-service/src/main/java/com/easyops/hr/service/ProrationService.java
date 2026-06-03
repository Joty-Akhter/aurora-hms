package com.easyops.hr.service;

import com.easyops.hr.entity.ProrationRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ES-28–ES-30: Proration for mid-period join/leave.
 * Uses join date (hire) and relieving date (termination) to compute days worked;
 * applies component proration rule (BY_DAYS, NO_PRORATION, BY_HOURS).
 */
public final class ProrationService {

    private ProrationService() {}

    /**
     * ES-30: Compute days worked in the period.
     * Uses employee join date (hireDate) and relieving date (terminationDate), or optional overrides.
     *
     * @param periodStart first day of pay period (inclusive)
     * @param periodEnd   last day of pay period (inclusive)
     * @param hireDate    employee join date (from master)
     * @param terminationDate employee relieving date (null if still active)
     * @param joinOverride    optional period-specific join date (e.g. for mid-period transfer); null = use hireDate
     * @param relievingOverride optional period-specific relieving date; null = use terminationDate or periodEnd
     * @return number of days in the period the employee was employed (0 if not in period)
     */
    public static int daysWorkedInPeriod(
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate hireDate,
            LocalDate terminationDate,
            LocalDate joinOverride,
            LocalDate relievingOverride) {
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            return 0;
        }
        LocalDate effectiveJoin = joinOverride != null ? joinOverride : hireDate;
        LocalDate effectiveRelieving = relievingOverride != null ? relievingOverride : terminationDate;

        if (effectiveJoin == null) {
            effectiveJoin = periodStart;
        }
        if (effectiveRelieving != null && effectiveRelieving.isBefore(periodStart)) {
            return 0;
        }
        if (effectiveJoin.isAfter(periodEnd)) {
            return 0;
        }

        LocalDate from = effectiveJoin.isBefore(periodStart) ? periodStart : effectiveJoin;
        LocalDate to = effectiveRelieving == null || effectiveRelieving.isAfter(periodEnd)
                ? periodEnd
                : effectiveRelieving;
        if (from.isAfter(to)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    /**
     * Total calendar days in the period (for BY_DAYS proration: amount × daysWorked / totalDays).
     */
    public static int totalDaysInPeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
    }

    /**
     * ES-28, ES-29: Apply proration rule to a component amount.
     *
     * @param fullAmount   full-period amount (e.g. monthly gross for the component)
     * @param rule         BY_DAYS, NO_PRORATION, or BY_HOURS
     * @param daysWorked   days worked in period (from daysWorkedInPeriod)
     * @param totalDays    total days in period (from totalDaysInPeriod)
     * @param hoursWorked  optional; used when rule is BY_HOURS (e.g. from time/attendance); null = fallback to days ratio
     * @param totalHours   optional; required for BY_HOURS when hoursWorked is provided
     * @return prorated amount (rounded to 2 decimals)
     */
    public static BigDecimal prorate(
            BigDecimal fullAmount,
            ProrationRule rule,
            int daysWorked,
            int totalDays,
            BigDecimal hoursWorked,
            BigDecimal totalHours) {
        if (fullAmount == null || fullAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (rule == null) {
            rule = ProrationRule.BY_DAYS;
        }

        switch (rule) {
            case NO_PRORATION:
                return fullAmount.setScale(2, RoundingMode.HALF_UP);
            case BY_HOURS:
                if (hoursWorked != null && totalHours != null && totalHours.compareTo(BigDecimal.ZERO) > 0) {
                    return fullAmount.multiply(hoursWorked).divide(totalHours, 2, RoundingMode.HALF_UP);
                }
                return prorateByDays(fullAmount, daysWorked, totalDays);
            case BY_DAYS:
            default:
                return prorateByDays(fullAmount, daysWorked, totalDays);
        }
    }

    private static BigDecimal prorateByDays(BigDecimal fullAmount, int daysWorked, int totalDays) {
        if (totalDays <= 0) {
            return BigDecimal.ZERO;
        }
        if (daysWorked >= totalDays) {
            return fullAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return fullAmount
                .multiply(BigDecimal.valueOf(daysWorked))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }
}
