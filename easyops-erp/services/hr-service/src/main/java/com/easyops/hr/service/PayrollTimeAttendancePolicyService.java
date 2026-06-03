package com.easyops.hr.service;

import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.repository.PayrollTimeAttendancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Loads or creates per-organization {@link PayrollTimeAttendancePolicy} with configurable OT multiplier,
 * missing-weekday LOP inference, and standard hours per day for hourly-rate derivation.
 */
@Service
@RequiredArgsConstructor
public class PayrollTimeAttendancePolicyService {

    private final PayrollTimeAttendancePolicyRepository repository;

    @Value("${hr.payroll.default-overtime-rate-multiplier:1.5}")
    private BigDecimal defaultOvertimeRateMultiplier;

    @Value("${hr.payroll.default-infer-missing-weekday-lop:true}")
    private boolean defaultInferMissingWeekdayLop;

    @Value("${hr.payroll.default-standard-hours-per-day:8}")
    private BigDecimal defaultStandardHoursPerDay;

    @Transactional
    public PayrollTimeAttendancePolicy getOrCreate(UUID organizationId) {
        return repository.findById(organizationId).orElseGet(() -> {
            PayrollTimeAttendancePolicy p = PayrollTimeAttendancePolicy.builder()
                    .organizationId(organizationId)
                    .overtimeRateMultiplier(defaultOvertimeRateMultiplier)
                    .inferMissingWeekdayLop(defaultInferMissingWeekdayLop)
                    .standardHoursPerDay(defaultStandardHoursPerDay)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return repository.save(p);
        });
    }

    @Transactional
    public PayrollTimeAttendancePolicy update(UUID organizationId, BigDecimal overtimeRateMultiplier,
                                              Boolean inferMissingWeekdayLop, BigDecimal standardHoursPerDay,
                                              Boolean leavePayrollBridgeEnabled,
                                              Boolean unpaidApprovedLeaveCountsAsLop,
                                              Boolean excludeActiveHolidaysFromWorkingDays,
                                              Boolean excludeActiveHolidaysFromLopInference) {
        PayrollTimeAttendancePolicy p = getOrCreate(organizationId);
        if (overtimeRateMultiplier != null && overtimeRateMultiplier.compareTo(BigDecimal.ZERO) > 0) {
            p.setOvertimeRateMultiplier(overtimeRateMultiplier);
        }
        if (inferMissingWeekdayLop != null) {
            p.setInferMissingWeekdayLop(inferMissingWeekdayLop);
        }
        if (standardHoursPerDay != null && standardHoursPerDay.compareTo(BigDecimal.ZERO) > 0) {
            p.setStandardHoursPerDay(standardHoursPerDay);
        }
        if (leavePayrollBridgeEnabled != null) {
            p.setLeavePayrollBridgeEnabled(leavePayrollBridgeEnabled);
        }
        if (unpaidApprovedLeaveCountsAsLop != null) {
            p.setUnpaidApprovedLeaveCountsAsLop(unpaidApprovedLeaveCountsAsLop);
        }
        if (excludeActiveHolidaysFromWorkingDays != null) {
            p.setExcludeActiveHolidaysFromWorkingDays(excludeActiveHolidaysFromWorkingDays);
        }
        if (excludeActiveHolidaysFromLopInference != null) {
            p.setExcludeActiveHolidaysFromLopInference(excludeActiveHolidaysFromLopInference);
        }
        p.setUpdatedAt(LocalDateTime.now());
        return repository.save(p);
    }
}
