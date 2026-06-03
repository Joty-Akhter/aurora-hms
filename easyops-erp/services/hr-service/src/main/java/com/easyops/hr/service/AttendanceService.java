package com.easyops.hr.service;

import com.easyops.hr.entity.AttendanceRecord;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.repository.AttendanceRecordRepository;
import com.easyops.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public List<AttendanceRecord> getAllAttendance(UUID organizationId) {
        return attendanceRepository.findByOrganizationId(organizationId);
    }

    public AttendanceRecord getAttendanceById(UUID attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
    }

    public List<AttendanceRecord> getEmployeeAttendance(UUID employeeId, UUID organizationId) {
        return attendanceRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }

    public List<AttendanceRecord> getTodayAttendance(UUID organizationId, LocalDate date) {
        return attendanceRepository.findTodayAttendance(organizationId, date);
    }

    public AttendanceRecord clockIn(UUID organizationId, UUID employeeId, UUID userId, String workLocation) {
        LocalDate today = LocalDate.now();

        Employee employee = resolveEmployee(organizationId, employeeId, userId);

        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getEmployeeId(), today)
                .orElseGet(() -> {
                    log.info("Clocking in employee {} (userId: {}) for organization {}", employee.getEmployeeId(), employee.getUserId(), organizationId);
                    AttendanceRecord record = new AttendanceRecord();
                    record.setEmployeeId(employee.getEmployeeId());
                    record.setOrganizationId(employee.getOrganizationId());
                    record.setAttendanceDate(today);
                    record.setClockInTime(LocalDateTime.now());
                    record.setWorkLocation(workLocation);
                    record.setStatus("present");
                    return attendanceRepository.save(record);
                });
    }

    private Employee resolveEmployee(UUID organizationId, UUID employeeId, UUID userId) {
        if (employeeId != null) {
            return employeeRepository.findById(employeeId)
                    .filter(e -> organizationId.equals(e.getOrganizationId()))
                    .orElseGet(() -> {
                        Optional<Employee> byUserId = employeeRepository.findByOrganizationIdAndUserId(organizationId, employeeId);
                        if (byUserId.isPresent()) {
                            Employee resolved = byUserId.get();
                            log.warn("Clock-in payload supplied userId {} in employeeId field. Resolved employee {}", employeeId, resolved.getEmployeeId());
                            return resolved;
                        }

                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Employee not found for this organization. Please create the employee before clocking in."
                        );
                    });
        }

        if (userId != null) {
            return employeeRepository.findByOrganizationIdAndUserId(organizationId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Employee not found for provided user. Please link the employee before clocking in."
                    ));
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Either employeeId or userId must be provided to clock in."
        );
    }

    public AttendanceRecord clockOut(UUID employeeId) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No clock-in record found for today"));

        record.setClockOutTime(LocalDateTime.now());

        if (record.getClockInTime() != null && record.getClockOutTime() != null) {
            Duration duration = Duration.between(record.getClockInTime(), record.getClockOutTime());
            double hours = duration.toMinutes() / 60.0;

            if (record.getBreakStartTime() != null && record.getBreakEndTime() != null) {
                Duration breakDuration = Duration.between(record.getBreakStartTime(), record.getBreakEndTime());
                hours -= breakDuration.toMinutes() / 60.0;
            }

            record.setTotalHours(BigDecimal.valueOf(hours));

            if (hours <= 8) {
                record.setRegularHours(BigDecimal.valueOf(hours));
                record.setOvertimeHours(BigDecimal.ZERO);
            } else {
                record.setRegularHours(BigDecimal.valueOf(8));
                record.setOvertimeHours(BigDecimal.valueOf(hours - 8));
            }
        }

        return attendanceRepository.save(record);
    }

    public AttendanceRecord startBreak(UUID employeeId) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No clock-in record found for today"));

        record.setBreakStartTime(LocalDateTime.now());
        return attendanceRepository.save(record);
    }

    public AttendanceRecord endBreak(UUID employeeId) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No clock-in record found for today"));

        record.setBreakEndTime(LocalDateTime.now());
        return attendanceRepository.save(record);
    }

    public AttendanceRecord createAttendance(AttendanceRecord attendance) {
        log.info("Creating attendance record for employee: {}", attendance.getEmployeeId());
        return attendanceRepository.save(attendance);
    }

    public AttendanceRecord updateAttendance(UUID attendanceId, AttendanceRecord attendance) {
        AttendanceRecord existing = getAttendanceById(attendanceId);

        if (attendance.getClockInTime() != null) existing.setClockInTime(attendance.getClockInTime());
        if (attendance.getClockOutTime() != null) existing.setClockOutTime(attendance.getClockOutTime());
        if (attendance.getBreakStartTime() != null) existing.setBreakStartTime(attendance.getBreakStartTime());
        if (attendance.getBreakEndTime() != null) existing.setBreakEndTime(attendance.getBreakEndTime());
        if (attendance.getTotalHours() != null) existing.setTotalHours(attendance.getTotalHours());
        if (attendance.getRegularHours() != null) existing.setRegularHours(attendance.getRegularHours());
        if (attendance.getOvertimeHours() != null) existing.setOvertimeHours(attendance.getOvertimeHours());
        if (attendance.getStatus() != null) existing.setStatus(attendance.getStatus());
        if (attendance.getWorkLocation() != null) existing.setWorkLocation(attendance.getWorkLocation());
        if (attendance.getNotes() != null) existing.setNotes(attendance.getNotes());

        return attendanceRepository.save(existing);
    }

    public void deleteAttendance(UUID attendanceId) {
        attendanceRepository.deleteById(attendanceId);
    }

    public List<AttendanceRecord> getAttendanceInRange(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByOrganizationIdAndAttendanceDateBetween(organizationId, startDate, endDate);
    }

    public List<AttendanceRecord> getEmployeeAttendanceInRange(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findEmployeeAttendanceInRange(employeeId, startDate, endDate);
    }
}
