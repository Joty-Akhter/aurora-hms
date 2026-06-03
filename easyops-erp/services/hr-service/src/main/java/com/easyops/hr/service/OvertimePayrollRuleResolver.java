package com.easyops.hr.service;

import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.entity.ShiftDefinition;
import com.easyops.hr.entity.ShiftSchedule;
import com.easyops.hr.repository.ShiftDefinitionRepository;
import com.easyops.hr.repository.ShiftScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HR-AT-05: Resolve OT standard hours and multiplier with precedence:
 * employee overrides → dominant roster shift definition → org {@link PayrollTimeAttendancePolicy}.
 */
@Service
@RequiredArgsConstructor
public class OvertimePayrollRuleResolver {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final ShiftDefinitionRepository shiftDefinitionRepository;

    private static final BigDecimal DEFAULT_OT_MULT = new BigDecimal("1.5");
    private static final BigDecimal DEFAULT_STD_HOURS = new BigDecimal("8");

    public BigDecimal resolveOvertimeRateMultiplier(
            Employee emp,
            LocalDate periodStart,
            LocalDate periodEnd,
            PayrollTimeAttendancePolicy policy) {
        if (emp.getPayrollOvertimeRateMultiplier() != null
                && emp.getPayrollOvertimeRateMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            return emp.getPayrollOvertimeRateMultiplier();
        }
        UUID dominantShiftDefId = dominantShiftDefinitionId(emp.getEmployeeId(), periodStart, periodEnd);
        if (dominantShiftDefId != null) {
            ShiftDefinition def = shiftDefinitionRepository.findById(dominantShiftDefId).orElse(null);
            if (def != null
                    && def.getOvertimeRateMultiplier() != null
                    && def.getOvertimeRateMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                return def.getOvertimeRateMultiplier();
            }
        }
        if (policy != null
                && policy.getOvertimeRateMultiplier() != null
                && policy.getOvertimeRateMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            return policy.getOvertimeRateMultiplier();
        }
        return DEFAULT_OT_MULT;
    }

    public BigDecimal resolveStandardHoursPerDay(
            Employee emp,
            LocalDate periodStart,
            LocalDate periodEnd,
            PayrollTimeAttendancePolicy policy) {
        if (emp.getPayrollStandardHoursPerDay() != null
                && emp.getPayrollStandardHoursPerDay().compareTo(BigDecimal.ZERO) > 0) {
            return emp.getPayrollStandardHoursPerDay();
        }
        UUID dominantShiftDefId = dominantShiftDefinitionId(emp.getEmployeeId(), periodStart, periodEnd);
        if (dominantShiftDefId != null) {
            ShiftDefinition def = shiftDefinitionRepository.findById(dominantShiftDefId).orElse(null);
            if (def != null
                    && def.getExpectedHours() != null
                    && def.getExpectedHours().compareTo(BigDecimal.ZERO) > 0) {
                return def.getExpectedHours();
            }
        }
        if (policy != null
                && policy.getStandardHoursPerDay() != null
                && policy.getStandardHoursPerDay().compareTo(BigDecimal.ZERO) > 0) {
            return policy.getStandardHoursPerDay();
        }
        return DEFAULT_STD_HOURS;
    }

    /** Shift definition id that appears most often on roster rows in range; tie-break by UUID for stability. */
    private UUID dominantShiftDefinitionId(UUID employeeId, LocalDate start, LocalDate end) {
        if (employeeId == null || start == null || end == null || end.isBefore(start)) {
            return null;
        }
        List<ShiftSchedule> rows = shiftScheduleRepository.findEmployeeShiftSchedulesInRange(employeeId, start, end);
        Map<UUID, Long> counts = rows.stream()
                .map(ShiftSchedule::getShiftDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (counts.isEmpty()) {
            return null;
        }
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
