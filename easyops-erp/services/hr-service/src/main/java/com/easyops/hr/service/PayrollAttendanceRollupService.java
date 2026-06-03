package com.easyops.hr.service;

import com.easyops.hr.dto.PayrollAttendanceRollup;
import com.easyops.hr.entity.AttendanceRecord;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.Holiday;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.entity.LeaveType;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.entity.Timesheet;
import com.easyops.hr.repository.AttendanceRecordRepository;
import com.easyops.hr.repository.HolidayRepository;
import com.easyops.hr.repository.LeaveRequestRepository;
import com.easyops.hr.repository.LeaveTypeRepository;
import com.easyops.hr.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rolls up daily attendance and approved timesheets for a pay period into working/present/leave/LOP days
 * and overtime hours. Overtime from daily attendance takes precedence over timesheet totals when both exist.
 * Missing weekdays (no attendance row) can optionally count as LOP per {@link PayrollTimeAttendancePolicy}.
 * Phase B (HR-LV-03): optional bridge from approved leave to suppress inferred LOP for paid leave days;
 * Phase B (HR-LV-02): optional holiday exclusions for working-day denominator and LOP inference.
 */
@Service
@RequiredArgsConstructor
public class PayrollAttendanceRollupService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final TimesheetRepository timesheetRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final HolidayRepository holidayRepository;

    /**
     * @param organizationId owning org
     * @param policy       may be null — defaults to infer missing weekday LOP and 1.5 OT multiplier at calculation time
     */
    public PayrollAttendanceRollup rollupForPayPeriod(
            UUID organizationId,
            Employee employee,
            LocalDate periodStart,
            LocalDate periodEnd,
            PayrollTimeAttendancePolicy policy) {
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        if (employee == null || periodStart == null || periodEnd == null) {
            return PayrollAttendanceRollup.empty(0);
        }
        LocalDate start = periodStart;
        LocalDate end = periodEnd;
        if (end.isBefore(start)) {
            return PayrollAttendanceRollup.empty(0);
        }
        if (employee.getHireDate() != null && employee.getHireDate().isAfter(start)) {
            start = employee.getHireDate();
        }
        if (employee.getTerminationDate() != null && employee.getTerminationDate().isBefore(end)) {
            end = employee.getTerminationDate();
        }
        if (start.isAfter(end)) {
            return PayrollAttendanceRollup.empty(0);
        }

        boolean bridgeEnabled = policy != null && Boolean.TRUE.equals(policy.getLeavePayrollBridgeEnabled());
        List<LeaveRequest> approvedLeaves = bridgeEnabled
                ? leaveRequestRepository.findApprovedOverlappingPeriod(
                        organizationId, employee.getEmployeeId(), start, end)
                : List.of();
        Map<UUID, LeaveType> leaveTypesById = bridgeEnabled && !approvedLeaves.isEmpty()
                ? loadLeaveTypes(approvedLeaves)
                : Map.of();

        List<Holiday> holidaysInRange = holidayRepository.findHolidaysInRange(organizationId, start, end);
        Set<LocalDate> applicableHolidayDates = holidaysInRange.stream()
                .filter(h -> holidayAppliesToEmployee(h, employee))
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toCollection(HashSet::new));

        int rawWeekdays = countWeekdays(start, end);
        int workingDays = rawWeekdays;
        if (policy != null && Boolean.TRUE.equals(policy.getExcludeActiveHolidaysFromWorkingDays())) {
            workingDays -= countWeekdaysInDateSet(applicableHolidayDates, start, end);
        }
        if (workingDays <= 0) {
            return PayrollAttendanceRollup.empty(Math.max(workingDays, 0));
        }

        boolean inferMissing = policy == null || Boolean.TRUE.equals(policy.getInferMissingWeekdayLop());
        boolean excludeHolidayFromLopInference =
                policy != null && Boolean.TRUE.equals(policy.getExcludeActiveHolidaysFromLopInference());
        boolean unpaidApprovedAddsLop = policy == null || !Boolean.FALSE.equals(policy.getUnpaidApprovedLeaveCountsAsLop());

        List<AttendanceRecord> attendance = attendanceRecordRepository.findEmployeeAttendanceInRange(
                employee.getEmployeeId(), start, end);
        List<Timesheet> timesheets = timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(
                employee.getEmployeeId(), start, end);

        boolean hasData = !attendance.isEmpty() || !timesheets.isEmpty()
                || (bridgeEnabled && !approvedLeaves.isEmpty());
        if (!hasData) {
            return new PayrollAttendanceRollup(workingDays, null, null, null, null, false);
        }

        BigDecimal leaveDays = BigDecimal.ZERO;
        BigDecimal lopDays = BigDecimal.ZERO;
        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal otFromAttendance = BigDecimal.ZERO;

        Set<LocalDate> datesWithAttendanceRow = attendance.stream()
                .map(AttendanceRecord::getAttendanceDate)
                .collect(Collectors.toCollection(HashSet::new));

        for (AttendanceRecord r : attendance) {
            LocalDate attendanceDate = r.getAttendanceDate();
            String st = normalizeStatus(r.getStatus());
            Kind classified = classifyAttendanceStatus(st);
            // HR-LV-03 / AC-11: reconcile attendance tokens with approved leave when bridge is on.
            if (bridgeEnabled && attendanceDate != null && !approvedLeaves.isEmpty()) {
                boolean paidCov = coveredByApprovedLeave(attendanceDate, approvedLeaves, leaveTypesById, true);
                boolean unpaidCov = coveredByApprovedLeave(attendanceDate, approvedLeaves, leaveTypesById, false);
                if (classified == Kind.LOP && paidCov) {
                    classified = Kind.PAID_LEAVE;
                } else if (classified == Kind.PAID_LEAVE && unpaidCov && !paidCov) {
                    classified = Kind.LOP;
                }
            }
            switch (classified) {
                case PRESENT -> presentDays = presentDays.add(BigDecimal.ONE);
                case HALF_PRESENT -> presentDays = presentDays.add(new BigDecimal("0.5"));
                case PAID_LEAVE -> leaveDays = leaveDays.add(BigDecimal.ONE);
                case LOP -> lopDays = lopDays.add(BigDecimal.ONE);
                case IGNORE -> {
                }
            }
            if (r.getOvertimeHours() != null) {
                otFromAttendance = otFromAttendance.add(r.getOvertimeHours());
            }
        }

        if (inferMissing || bridgeEnabled) {
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                DayOfWeek w = d.getDayOfWeek();
                if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) {
                    continue;
                }
                if (excludeHolidayFromLopInference && applicableHolidayDates.contains(d)) {
                    continue;
                }
                if (datesWithAttendanceRow.contains(d)) {
                    continue;
                }
                if (bridgeEnabled) {
                    boolean paidCov = coveredByApprovedLeave(d, approvedLeaves, leaveTypesById, true);
                    boolean unpaidCov = coveredByApprovedLeave(d, approvedLeaves, leaveTypesById, false);
                    if (paidCov) {
                        leaveDays = leaveDays.add(BigDecimal.ONE);
                        continue;
                    }
                    if (unpaidCov) {
                        if (inferMissing && unpaidApprovedAddsLop) {
                            lopDays = lopDays.add(BigDecimal.ONE);
                        }
                        continue;
                    }
                }
                if (inferMissing) {
                    lopDays = lopDays.add(BigDecimal.ONE);
                }
            }
        }

        BigDecimal otFromTimesheet = BigDecimal.ZERO;
        for (Timesheet t : timesheets) {
            if (!isTimesheetCountingForPayroll(t)) {
                continue;
            }
            if (t.getOvertimeHours() != null) {
                otFromTimesheet = otFromTimesheet.add(t.getOvertimeHours());
            }
        }

        BigDecimal overtimeHours;
        if (otFromAttendance.compareTo(BigDecimal.ZERO) > 0) {
            overtimeHours = otFromAttendance;
        } else {
            overtimeHours = otFromTimesheet.compareTo(BigDecimal.ZERO) > 0 ? otFromTimesheet : null;
        }

        return new PayrollAttendanceRollup(
                workingDays,
                presentDays,
                leaveDays,
                lopDays,
                overtimeHours,
                true
        );
    }

    static int countWeekdays(LocalDate start, LocalDate end) {
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                n++;
            }
        }
        return n;
    }

    static int countWeekdaysInDateSet(Set<LocalDate> dates, LocalDate start, LocalDate end) {
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) {
                continue;
            }
            if (dates.contains(d)) {
                n++;
            }
        }
        return n;
    }

    private static boolean holidayAppliesToEmployee(Holiday h, Employee emp) {
        if (h.getEmployeeId() != null) {
            return emp.getEmployeeId() != null && h.getEmployeeId().equals(emp.getEmployeeId());
        }
        if (h.getDepartmentId() != null) {
            return emp.getDepartmentId() != null && h.getDepartmentId().equals(emp.getDepartmentId());
        }
        return true;
    }

    private Map<UUID, LeaveType> loadLeaveTypes(List<LeaveRequest> approvedLeaves) {
        Set<UUID> ids = approvedLeaves.stream().map(LeaveRequest::getLeaveTypeId).collect(Collectors.toSet());
        Map<UUID, LeaveType> map = new HashMap<>();
        leaveTypeRepository.findAllById(ids).forEach(lt -> map.put(lt.getLeaveTypeId(), lt));
        return map;
    }

    /**
     * @param paid true = paid leave types only; false = explicitly unpaid ({@code isPaid == false})
     */
    private static boolean coveredByApprovedLeave(
            LocalDate d,
            List<LeaveRequest> approvedLeaves,
            Map<UUID, LeaveType> leaveTypesById,
            boolean paid) {
        for (LeaveRequest lr : approvedLeaves) {
            if (lr.getStartDate() != null && lr.getEndDate() != null
                    && !d.isBefore(lr.getStartDate()) && !d.isAfter(lr.getEndDate())) {
                LeaveType lt = leaveTypesById.get(lr.getLeaveTypeId());
                boolean typePaid = isPaidLeaveType(lt);
                if (paid && typePaid) {
                    return true;
                }
                if (!paid && lt != null && Boolean.FALSE.equals(lt.getIsPaid())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Null or unknown leave type treated as paid (safe default). */
    private static boolean isPaidLeaveType(LeaveType lt) {
        return lt == null || lt.getIsPaid() == null || Boolean.TRUE.equals(lt.getIsPaid());
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private enum Kind {
        PRESENT, HALF_PRESENT, PAID_LEAVE, LOP, IGNORE
    }

    private static Kind classifyAttendanceStatus(String st) {
        if (st.isEmpty()) {
            return Kind.IGNORE;
        }
        if (matches(st, Set.of("present", "present_wfh", "wfh", "work_from_home"))) {
            return Kind.PRESENT;
        }
        if (matches(st, Set.of("half_day", "halfday", "half_day_present"))) {
            return Kind.HALF_PRESENT;
        }
        if (matches(st, Set.of("paid_leave", "sick_leave", "sick_paid", "annual_leave", "casual_leave", "leave", "on_leave"))) {
            return Kind.PAID_LEAVE;
        }
        if (matches(st, Set.of("absent", "lop", "leave_without_pay", "unpaid_leave", "lwop", "unpaid"))) {
            return Kind.LOP;
        }
        return Kind.IGNORE;
    }

    private static boolean matches(String st, Set<String> keys) {
        return keys.contains(st);
    }

    private static boolean isTimesheetCountingForPayroll(Timesheet t) {
        String s = t.getStatus();
        if (s == null) {
            return false;
        }
        String u = s.trim().toUpperCase(Locale.ROOT);
        return "SUBMITTED".equals(u) || "APPROVED".equals(u) || "PROCESSED".equals(u);
    }
}
