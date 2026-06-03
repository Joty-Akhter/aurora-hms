package com.easyops.hr.controller;

import com.easyops.hr.dto.DevicePunchDto;
import com.easyops.hr.dto.DeviceSyncResultDto;
import com.easyops.hr.entity.AttendanceRawLog;
import com.easyops.hr.entity.AttendanceRecord;
import com.easyops.hr.repository.AttendanceRawLogRepository;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.AttendanceService;
import com.easyops.hr.service.DeviceAttendanceSyncService;
import com.easyops.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/attendance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final HrRbacService hrRbac;
    private final DeviceAttendanceSyncService deviceSyncService;
    private final AttendanceRawLogRepository rawLogRepository;
    
    @GetMapping
    public ResponseEntity<List<AttendanceRecord>> getAllAttendance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /attendance - organizationId: {}, employeeId: {}", organizationId, employeeId);

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgForRbac = (employeeId != null)
                ? employeeService.getEmployeeById(employeeId).getOrganizationId()
                : organizationId;
        hrRbac.requireHrView(actor, orgForRbac);
        
        List<AttendanceRecord> records;
        
        if (startDate != null && endDate != null) {
            if (employeeId != null) {
                records = attendanceService.getEmployeeAttendanceInRange(employeeId, startDate, endDate);
            } else {
                records = attendanceService.getAttendanceInRange(organizationId, startDate, endDate);
            }
        } else if (employeeId != null) {
            records = attendanceService.getEmployeeAttendance(employeeId, organizationId);
        } else {
            records = attendanceService.getAllAttendance(organizationId);
        }
        
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AttendanceRecord> getAttendanceById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /attendance/{}", id);
        AttendanceRecord record = attendanceService.getAttendanceById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, record.getOrganizationId());
        return ResponseEntity.ok(record);
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<AttendanceRecord>> getTodayAttendance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("GET /attendance/today - organizationId: {}, date: {}", organizationId, targetDate);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<AttendanceRecord> records = attendanceService.getTodayAttendance(organizationId, targetDate);
        return ResponseEntity.ok(records);
    }
    
    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceRecord> clockIn(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UUID organizationId = parseUuid(request.get("organizationId"), "organizationId", true);
        UUID employeeId = parseUuid(request.get("employeeId"), "employeeId", false);
        UUID userId = parseUuid(request.get("userId"), "userId", false);
        String workLocation = (String) request.get("workLocation");

        if (employeeId == null && userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either employeeId or userId must be provided.");
        }
        
        log.info("POST /attendance/clock-in - employeeId: {}, userId: {}", employeeId, userId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        AttendanceRecord record = attendanceService.clockIn(organizationId, employeeId, userId, workLocation);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceRecord> clockOut(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, String> request) {
        UUID employeeId = UUID.fromString(request.get("employeeId"));
        
        log.info("POST /attendance/clock-out - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        hrRbac.requireHrManage(actor, org);
        AttendanceRecord record = attendanceService.clockOut(employeeId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/break-start")
    public ResponseEntity<AttendanceRecord> startBreak(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, String> request) {
        UUID employeeId = UUID.fromString(request.get("employeeId"));
        
        log.info("POST /attendance/break-start - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        hrRbac.requireHrManage(actor, org);
        AttendanceRecord record = attendanceService.startBreak(employeeId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/break-end")
    public ResponseEntity<AttendanceRecord> endBreak(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, String> request) {
        UUID employeeId = UUID.fromString(request.get("employeeId"));
        
        log.info("POST /attendance/break-end - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID org = employeeService.getEmployeeById(employeeId).getOrganizationId();
        hrRbac.requireHrManage(actor, org);
        AttendanceRecord record = attendanceService.endBreak(employeeId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping
    public ResponseEntity<AttendanceRecord> createAttendance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody AttendanceRecord attendance) {
        log.info("POST /attendance - Creating attendance record");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, attendance.getOrganizationId());
        AttendanceRecord created = attendanceService.createAttendance(attendance);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceRecord> updateAttendance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody AttendanceRecord attendance) {
        log.info("PUT /attendance/{}", id);
        AttendanceRecord existing = attendanceService.getAttendanceById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        AttendanceRecord updated = attendanceService.updateAttendance(id, attendance);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("DELETE /attendance/{}", id);
        AttendanceRecord existing = attendanceService.getAttendanceById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------
    // HR-AT-03: Device / biometric attendance integration
    // ---------------------------------------------------------------

    /**
     * Ingest raw punch events from a biometric or access-control device.
     * Punches are stored in attendance_raw_logs and not yet reflected in attendance_records.
     * Call {@code POST /device-sync/process} afterwards to fold them into attendance records.
     */
    @PostMapping("/device-sync")
    public ResponseEntity<DeviceSyncResultDto> deviceSync(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestBody List<DevicePunchDto> punches) {
        log.info("POST /attendance/device-sync - organizationId: {}, punches: {}", organizationId, punches.size());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        DeviceSyncResultDto result = deviceSyncService.ingestPunches(organizationId, punches);
        return ResponseEntity.ok(result);
    }

    /**
     * Process all unprocessed raw punches in a date range into attendance records.
     * Existing attendance records for the same employee+day are updated (not duplicated).
     */
    @PostMapping("/device-sync/process")
    public ResponseEntity<DeviceSyncResultDto> processDeviceLogs(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal standardHoursPerDay) {
        log.info("POST /attendance/device-sync/process - org: {}, from: {}, to: {}", organizationId, from, to);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        DeviceSyncResultDto result = deviceSyncService.processRawLogs(organizationId, from, to, standardHoursPerDay);
        return ResponseEntity.ok(result);
    }

    /**
     * View raw device punch logs for a date/time range (for audit and review).
     */
    @GetMapping("/device-logs")
    public ResponseEntity<List<AttendanceRawLog>> getDeviceLogs(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("GET /attendance/device-logs - org: {}, from: {}, to: {}", organizationId, from, to);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<AttendanceRawLog> logs;
        if (from != null && to != null) {
            logs = rawLogRepository.findByOrganizationIdAndPunchTimeBetweenOrderByPunchTimeAsc(organizationId, from, to);
        } else {
            logs = rawLogRepository.findByOrganizationIdOrderByPunchTimeAsc(organizationId);
        }
        return ResponseEntity.ok(logs);
    }

    private UUID parseUuid(Object rawValue, String fieldName, boolean required) {
        if (rawValue == null) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
            }
            return null;
        }

        if (rawValue instanceof UUID uuid) {
            return uuid;
        }

        if (rawValue instanceof String value && !value.isBlank()) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID.");
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID.");
    }
}
