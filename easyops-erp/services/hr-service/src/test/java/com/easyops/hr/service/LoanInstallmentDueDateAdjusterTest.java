package com.easyops.hr.service;

import com.easyops.hr.entity.LoanHolidayShiftMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanInstallmentDueDateAdjusterTest {

    @Test
    void adjust_shiftDisabled_returnsCandidate() {
        LocalDate d = LocalDate.of(2024, 3, 2); // Saturday
        assertEquals(
                d,
                LoanInstallmentDueDateAdjuster.adjust(d, false, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, Set.of()));
    }

    @Test
    void adjust_saturday_nextBusinessDay_movesToMonday() {
        LocalDate sat = LocalDate.of(2024, 3, 2); // Saturday
        LocalDate expected = LocalDate.of(2024, 3, 4); // Monday
        assertEquals(
                expected,
                LoanInstallmentDueDateAdjuster.adjust(sat, true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, Set.of()));
    }

    @Test
    void adjust_saturday_previousBusinessDay_movesToFriday() {
        LocalDate sat = LocalDate.of(2024, 3, 2);
        LocalDate expected = LocalDate.of(2024, 3, 1); // Friday
        assertEquals(
                expected,
                LoanInstallmentDueDateAdjuster.adjust(sat, true, LoanHolidayShiftMode.PREVIOUS_BUSINESS_DAY, Set.of()));
    }

    @Test
    void adjust_orgHoliday_treatedAsNonWorking_next() {
        LocalDate d = LocalDate.of(2024, 3, 4); // Monday
        Set<LocalDate> holidays = Set.of(d);
        assertEquals(
                LocalDate.of(2024, 3, 5),
                LoanInstallmentDueDateAdjuster.adjust(d, true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, holidays));
    }

    @Test
    void adjust_sunday_nextBusinessDay_movesToMonday() {
        LocalDate sun = LocalDate.of(2024, 3, 3);
        assertEquals(
                LocalDate.of(2024, 3, 4),
                LoanInstallmentDueDateAdjuster.adjust(sun, true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, Set.of()));
    }

    @Test
    void adjust_weekdayUnchanged_whenShiftEnabled() {
        LocalDate wed = LocalDate.of(2024, 3, 6);
        assertEquals(
                wed,
                LoanInstallmentDueDateAdjuster.adjust(wed, true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, Set.of()));
    }

    @Test
    void adjust_nullMode_defaultsToNext() {
        LocalDate sat = LocalDate.of(2024, 3, 2);
        assertEquals(
                LocalDate.of(2024, 3, 4),
                LoanInstallmentDueDateAdjuster.adjust(sat, true, null, Set.of()));
    }

    @Test
    void adjust_excessiveHolidayDensity_throws() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        Set<LocalDate> wall = new HashSet<>();
        for (int i = 0; i < 90; i++) {
            wall.add(start.plusDays(i));
        }
        assertThrows(
                IllegalStateException.class,
                () -> LoanInstallmentDueDateAdjuster.adjust(
                        start, true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY, wall));
    }
}
