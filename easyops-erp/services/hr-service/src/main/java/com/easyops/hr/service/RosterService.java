package com.easyops.hr.service;

import com.easyops.hr.dto.roster.RosterConflictWarningDto;
import com.easyops.hr.dto.roster.RosterMonthViewDto;
import com.easyops.hr.dto.roster.RosterScheduleRowDto;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.Holiday;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.entity.ShiftDefinition;
import com.easyops.hr.entity.ShiftSchedule;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.HolidayRepository;
import com.easyops.hr.repository.LeaveRequestRepository;
import com.easyops.hr.repository.ShiftDefinitionRepository;
import com.easyops.hr.repository.ShiftScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HR-AT-02: Roster persistence and month planner overlay (holidays + approved leave + conflict hints).
 */
@Service
@RequiredArgsConstructor
public class RosterService {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final ShiftDefinitionRepository shiftDefinitionRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public ShiftSchedule getSchedule(UUID scheduleId) {
        return shiftScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "schedule_not_found"));
    }

    public List<ShiftSchedule> listSchedules(UUID organizationId, LocalDate start, LocalDate end, UUID departmentId) {
        validateRange(start, end);
        if (departmentId != null) {
            return shiftScheduleRepository.findByOrganizationDepartmentAndDateRange(organizationId, departmentId, start, end);
        }
        return shiftScheduleRepository.findShiftSchedulesInRange(organizationId, start, end);
    }

    public RosterMonthViewDto buildMonthView(UUID organizationId, int year, int month, UUID departmentId) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<ShiftSchedule> schedules = listSchedules(organizationId, start, end, departmentId);

        Set<UUID> employeeIds = schedules.stream().map(ShiftSchedule::getEmployeeId).collect(Collectors.toCollection(HashSet::new));
        Map<UUID, Employee> employeesById = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            employeeRepository.findAllById(employeeIds).forEach(e -> employeesById.put(e.getEmployeeId(), e));
        }

        Set<UUID> defIds = schedules.stream()
                .map(ShiftSchedule::getShiftDefinitionId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, ShiftDefinition> defsById = new HashMap<>();
        if (!defIds.isEmpty()) {
            shiftDefinitionRepository.findAllById(defIds).forEach(d -> defsById.put(d.getShiftDefinitionId(), d));
        }

        List<RosterScheduleRowDto> rows = new ArrayList<>();
        for (ShiftSchedule ss : schedules) {
            Employee emp = employeesById.get(ss.getEmployeeId());
            ShiftDefinition def = ss.getShiftDefinitionId() != null ? defsById.get(ss.getShiftDefinitionId()) : null;
            rows.add(new RosterScheduleRowDto(
                    ss.getScheduleId(),
                    ss.getEmployeeId(),
                    emp != null ? emp.getName() : null,
                    ss.getShiftDate(),
                    ss.getShiftDefinitionId(),
                    def != null ? def.getName() : null,
                    ss.getShiftName(),
                    ss.getStartTime(),
                    ss.getEndTime(),
                    ss.getBreakDuration(),
                    ss.getIsOvertime(),
                    ss.getNotes()));
        }

        List<Holiday> holidays = holidayRepository.findHolidaysInRange(organizationId, start, end);

        List<LeaveRequest> approvedLeaves = employeeIds.isEmpty()
                ? List.of()
                : leaveRequestRepository.findApprovedOverlappingForEmployees(
                        organizationId, employeeIds, start, end);

        List<RosterConflictWarningDto> warnings = buildWarnings(schedules, employeesById, holidays, approvedLeaves, start, end);

        return new RosterMonthViewDto(rows, holidays, approvedLeaves, warnings);
    }

    private List<RosterConflictWarningDto> buildWarnings(
            List<ShiftSchedule> schedules,
            Map<UUID, Employee> employeesById,
            List<Holiday> holidays,
            List<LeaveRequest> approvedLeaves,
            LocalDate rangeStart,
            LocalDate rangeEnd) {

        List<RosterConflictWarningDto> out = new ArrayList<>();

        Map<UUID, Set<LocalDate>> leaveDatesByEmployee = new HashMap<>();
        for (LeaveRequest lr : approvedLeaves) {
            if (lr.getEmployeeId() == null || lr.getStartDate() == null || lr.getEndDate() == null) {
                continue;
            }
            LocalDate d = lr.getStartDate().isBefore(rangeStart) ? rangeStart : lr.getStartDate();
            LocalDate end = lr.getEndDate().isAfter(rangeEnd) ? rangeEnd : lr.getEndDate();
            for (; !d.isAfter(end); d = d.plusDays(1)) {
                leaveDatesByEmployee.computeIfAbsent(lr.getEmployeeId(), k -> new HashSet<>()).add(d);
            }
        }

        for (ShiftSchedule ss : schedules) {
            Employee emp = employeesById.get(ss.getEmployeeId());
            if (emp == null || ss.getShiftDate() == null) {
                continue;
            }
            Set<LocalDate> ld = leaveDatesByEmployee.get(ss.getEmployeeId());
            if (ld != null && ld.contains(ss.getShiftDate())) {
                out.add(new RosterConflictWarningDto(
                        "LEAVE_OVERLAP",
                        ss.getEmployeeId(),
                        emp.getName(),
                        ss.getShiftDate(),
                        "Scheduled shift on a date with approved leave."));
            }
            DayOfWeek dow = ss.getShiftDate().getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                for (Holiday h : holidays) {
                    if (!Boolean.TRUE.equals(h.getIsActive()) || h.getHolidayDate() == null) {
                        continue;
                    }
                    if (!h.getHolidayDate().equals(ss.getShiftDate())) {
                        continue;
                    }
                    if (holidayAppliesToEmployee(h, emp)) {
                        out.add(new RosterConflictWarningDto(
                                "HOLIDAY_SHIFT",
                                ss.getEmployeeId(),
                                emp.getName(),
                                ss.getShiftDate(),
                                "Shift on " + h.getHolidayName() + " (" + h.getHolidayDate() + ")."));
                        break;
                    }
                }
            }
        }

        Map<String, Long> perDayCounts = schedules.stream()
                .filter(ss -> ss.getEmployeeId() != null && ss.getShiftDate() != null)
                .collect(Collectors.groupingBy(ss -> ss.getEmployeeId() + "|" + ss.getShiftDate(), Collectors.counting()));
        for (Map.Entry<String, Long> e : perDayCounts.entrySet()) {
            if (e.getValue() <= 1) {
                continue;
            }
            String[] parts = e.getKey().split("\\|");
            UUID empId = UUID.fromString(parts[0]);
            LocalDate d = LocalDate.parse(parts[1]);
            Employee emp = employeesById.get(empId);
            out.add(new RosterConflictWarningDto(
                    "DOUBLE_BOOKING",
                    empId,
                    emp != null ? emp.getName() : null,
                    d,
                    "More than one roster row for the same employee and date."));
        }

        return out;
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

    @Transactional
    public ShiftSchedule createSchedule(ShiftSchedule schedule) {
        validateSchedule(schedule);
        enforceSingleSchedulePerDay(schedule.getEmployeeId(), schedule.getShiftDate(), null);
        return shiftScheduleRepository.save(schedule);
    }

    @Transactional
    public ShiftSchedule updateSchedule(UUID scheduleId, ShiftSchedule patch) {
        ShiftSchedule existing = shiftScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "schedule_not_found"));
        if (patch.getEmployeeId() != null) {
            existing.setEmployeeId(patch.getEmployeeId());
        }
        if (patch.getShiftDefinitionId() != null) {
            existing.setShiftDefinitionId(patch.getShiftDefinitionId());
        }
        if (patch.getShiftDate() != null) {
            existing.setShiftDate(patch.getShiftDate());
        }
        if (patch.getShiftName() != null) {
            existing.setShiftName(patch.getShiftName());
        }
        if (patch.getStartTime() != null) {
            existing.setStartTime(patch.getStartTime());
        }
        if (patch.getEndTime() != null) {
            existing.setEndTime(patch.getEndTime());
        }
        if (patch.getBreakDuration() != null) {
            existing.setBreakDuration(patch.getBreakDuration());
        }
        if (patch.getIsOvertime() != null) {
            existing.setIsOvertime(patch.getIsOvertime());
        }
        if (patch.getNotes() != null) {
            existing.setNotes(patch.getNotes());
        }
        if (patch.getUpdatedBy() != null) {
            existing.setUpdatedBy(patch.getUpdatedBy());
        }
        validateSchedule(existing);
        enforceSingleSchedulePerDay(existing.getEmployeeId(), existing.getShiftDate(), scheduleId);
        return shiftScheduleRepository.save(existing);
    }

    private void validateSchedule(ShiftSchedule schedule) {
        if (schedule.getOrganizationId() == null || schedule.getEmployeeId() == null
                || schedule.getShiftDate() == null || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roster_missing_required_fields");
        }
        Employee emp = employeeRepository.findById(schedule.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "employee_not_found"));
        if (!emp.getOrganizationId().equals(schedule.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employee_org_mismatch");
        }
        if (schedule.getShiftDefinitionId() != null) {
            ShiftDefinition def = shiftDefinitionRepository.findById(schedule.getShiftDefinitionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "shift_definition_not_found"));
            if (!def.getOrganizationId().equals(schedule.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shift_definition_org_mismatch");
            }
        }
    }

    private void enforceSingleSchedulePerDay(UUID employeeId, LocalDate day, UUID excludeScheduleId) {
        List<ShiftSchedule> sameDay = shiftScheduleRepository.findByEmployeeIdAndShiftDate(employeeId, day);
        for (ShiftSchedule s : sameDay) {
            if (excludeScheduleId != null && s.getScheduleId().equals(excludeScheduleId)) {
                continue;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "roster_duplicate_employee_date");
        }
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_date_range");
        }
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId, UUID organizationId) {
        ShiftSchedule s = getSchedule(scheduleId);
        if (organizationId == null || !s.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organization_mismatch");
        }
        shiftScheduleRepository.deleteById(scheduleId);
    }
}
