package com.easyops.hr.service;

import com.easyops.hr.entity.LoanHolidayShiftMode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * AD-03: shift installment due dates off weekends and configured org holidays ({@code hr.holidays}).
 */
public final class LoanInstallmentDueDateAdjuster {

    private static final int MAX_SHIFT_DAYS = 62;

    private LoanInstallmentDueDateAdjuster() {
    }

    public static LocalDate adjust(
            LocalDate candidate,
            boolean shiftForHolidays,
            LoanHolidayShiftMode mode,
            Set<LocalDate> orgHolidayDates) {
        if (!shiftForHolidays) {
            return candidate;
        }
        LoanHolidayShiftMode m = mode != null ? mode : LoanHolidayShiftMode.NEXT_BUSINESS_DAY;
        Set<LocalDate> holidays = orgHolidayDates != null ? orgHolidayDates : Set.of();
        LocalDate d = candidate;
        int guard = 0;
        while (guard++ < MAX_SHIFT_DAYS && isNonWorkingDay(d, holidays)) {
            d = m == LoanHolidayShiftMode.PREVIOUS_BUSINESS_DAY ? d.minusDays(1) : d.plusDays(1);
        }
        if (isNonWorkingDay(d, holidays)) {
            throw new IllegalStateException(
                    "Could not reach a business day within " + MAX_SHIFT_DAYS + " days of " + candidate
                            + "; check for an unrealistically dense holiday calendar near that date.");
        }
        return d;
    }

    private static boolean isNonWorkingDay(LocalDate d, Set<LocalDate> holidays) {
        DayOfWeek dw = d.getDayOfWeek();
        if (dw == DayOfWeek.SATURDAY || dw == DayOfWeek.SUNDAY) {
            return true;
        }
        return holidays.contains(d);
    }
}
