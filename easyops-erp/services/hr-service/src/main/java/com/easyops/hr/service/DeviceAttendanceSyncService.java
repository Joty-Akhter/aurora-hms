package com.easyops.hr.service;

import com.easyops.hr.dto.DevicePunchDto;
import com.easyops.hr.dto.DeviceSyncResultDto;
import com.easyops.hr.entity.AttendanceRawLog;
import com.easyops.hr.entity.AttendanceRecord;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.repository.AttendanceRawLogRepository;
import com.easyops.hr.repository.AttendanceRecordRepository;
import com.easyops.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HR-AT-03: Ingest raw device/biometric punches and process them into {@link AttendanceRecord} rows.
 *
 * Two-phase workflow:
 *  1. {@link #ingestPunches} — saves raw {@link AttendanceRawLog} rows (one per device punch).
 *  2. {@link #processRawLogs} — pairs IN/OUT punches per employee per day and upserts attendance records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeviceAttendanceSyncService {

    private static final BigDecimal SCALE2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HALF_HOUR = new BigDecimal("0.5");
    private static final BigDecimal HALF_DAY_THRESHOLD = new BigDecimal("4.0");

    private final AttendanceRawLogRepository rawLogRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    // ---------------------------------------------------------------
    // Phase 1: Ingest
    // ---------------------------------------------------------------

    /**
     * Save raw device punches to {@code attendance_raw_logs}.
     * Attempts to resolve employeeId from rawEmployeeCode when UUID is absent.
     */
    public DeviceSyncResultDto ingestPunches(UUID organizationId, List<DevicePunchDto> punches) {
        if (punches == null || punches.isEmpty()) {
            return DeviceSyncResultDto.builder().ingested(0).skipped(0).warnings(List.of()).build();
        }

        int ingested = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (DevicePunchDto dto : punches) {
            if (dto.getPunchTime() == null) {
                skipped++;
                warnings.add("Skipped punch with null punchTime");
                continue;
            }

            UUID empId = dto.getEmployeeId();
            if (empId == null && dto.getRawEmployeeCode() != null) {
                empId = resolveByRawCode(organizationId, dto.getRawEmployeeCode());
                if (empId == null) {
                    warnings.add("Could not resolve employee for rawEmployeeCode=" + dto.getRawEmployeeCode()
                            + " at " + dto.getPunchTime());
                }
            }

            AttendanceRawLog log = new AttendanceRawLog();
            log.setOrganizationId(organizationId);
            log.setEmployeeId(empId);
            log.setDeviceId(dto.getDeviceId());
            log.setPunchTime(dto.getPunchTime());
            log.setPunchType(normalizePunchType(dto.getPunchType()));
            log.setSource("DEVICE");
            log.setRawEmployeeCode(dto.getRawEmployeeCode());
            log.setNotes(dto.getNotes());
            log.setProcessed(false);
            rawLogRepository.save(log);
            ingested++;
        }

        log.info("HR-AT-03: Ingested {} device punches for org {}; skipped={}", ingested, organizationId, skipped);
        return DeviceSyncResultDto.builder()
                .ingested(ingested)
                .attendanceRecordsAffected(0)
                .skipped(skipped)
                .warnings(warnings)
                .build();
    }

    // ---------------------------------------------------------------
    // Phase 2: Process raw logs → attendance records
    // ---------------------------------------------------------------

    /**
     * Process all unprocessed raw punches in [from, to] into attendance records.
     * Uses {@code standardHoursPerDay} (default 8) to split regular vs overtime hours.
     */
    public DeviceSyncResultDto processRawLogs(UUID organizationId, LocalDate from, LocalDate to,
                                               BigDecimal standardHoursPerDay) {
        BigDecimal stdHours = (standardHoursPerDay != null && standardHoursPerDay.compareTo(BigDecimal.ZERO) > 0)
                ? standardHoursPerDay : new BigDecimal("8");

        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        List<AttendanceRawLog> unprocessed = rawLogRepository.findUnprocessedInRange(organizationId, dtFrom, dtTo);
        if (unprocessed.isEmpty()) {
            return DeviceSyncResultDto.builder()
                    .ingested(0).attendanceRecordsAffected(0).skipped(0).warnings(List.of("No unprocessed punches found in range")).build();
        }

        // Group by (employeeId, date)
        Map<UUID, Map<LocalDate, List<AttendanceRawLog>>> byEmployeeByDay = unprocessed.stream()
                .filter(r -> r.getEmployeeId() != null)
                .collect(Collectors.groupingBy(
                        AttendanceRawLog::getEmployeeId,
                        Collectors.groupingBy(r -> r.getPunchTime().toLocalDate())));

        List<String> warnings = new ArrayList<>();
        int skipped = (int) unprocessed.stream().filter(r -> r.getEmployeeId() == null).count();
        if (skipped > 0) {
            warnings.add(skipped + " punch(es) skipped — employeeId not resolved.");
        }
        // Mark unresolved as processed so they don't appear again
        unprocessed.stream().filter(r -> r.getEmployeeId() == null).forEach(r -> {
            r.setProcessed(true);
            r.setProcessedAt(LocalDateTime.now());
        });

        int affected = 0;
        for (Map.Entry<UUID, Map<LocalDate, List<AttendanceRawLog>>> empEntry : byEmployeeByDay.entrySet()) {
            UUID empId = empEntry.getKey();
            for (Map.Entry<LocalDate, List<AttendanceRawLog>> dayEntry : empEntry.getValue().entrySet()) {
                LocalDate date = dayEntry.getKey();
                List<AttendanceRawLog> dayLogs = dayEntry.getValue();
                dayLogs.sort(Comparator.comparing(AttendanceRawLog::getPunchTime));

                AttendanceRecord record = upsertFromPunches(organizationId, empId, date, dayLogs, stdHours, warnings);
                UUID arId = record.getAttendanceId();
                dayLogs.forEach(r -> {
                    r.setProcessed(true);
                    r.setProcessedAt(LocalDateTime.now());
                    r.setAttendanceRecordId(arId);
                });
                affected++;
            }
        }

        rawLogRepository.saveAll(unprocessed);
        log.info("HR-AT-03: Processed raw punches for org {}; attendanceRecordsAffected={}, skipped={}",
                organizationId, affected, skipped);
        return DeviceSyncResultDto.builder()
                .ingested(unprocessed.size())
                .attendanceRecordsAffected(affected)
                .skipped(skipped)
                .warnings(warnings)
                .build();
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private AttendanceRecord upsertFromPunches(UUID organizationId, UUID empId, LocalDate date,
                                                List<AttendanceRawLog> dayLogs,
                                                BigDecimal stdHours, List<String> warnings) {
        // Determine clock-in and clock-out times
        Optional<LocalDateTime> firstIn = dayLogs.stream()
                .filter(r -> "IN".equals(r.getPunchType()))
                .map(AttendanceRawLog::getPunchTime)
                .min(Comparator.naturalOrder());
        Optional<LocalDateTime> lastOut = dayLogs.stream()
                .filter(r -> "OUT".equals(r.getPunchType()))
                .map(AttendanceRawLog::getPunchTime)
                .max(Comparator.naturalOrder());

        // Fallback: if no typed IN/OUT, use first as IN and last as OUT
        LocalDateTime clockIn = firstIn.orElseGet(() -> dayLogs.get(0).getPunchTime());
        LocalDateTime clockOut = lastOut.orElseGet(() -> dayLogs.get(dayLogs.size() - 1).getPunchTime());

        // Require at least one gap (same punch = no meaningful data)
        BigDecimal totalHours = SCALE2;
        BigDecimal regularHours = SCALE2;
        BigDecimal overtimeHours = SCALE2;

        if (clockOut.isAfter(clockIn)) {
            double rawHours = Duration.between(clockIn, clockOut).toMinutes() / 60.0;
            totalHours = BigDecimal.valueOf(rawHours).setScale(2, RoundingMode.HALF_UP);
            if (totalHours.compareTo(stdHours) <= 0) {
                regularHours = totalHours;
                overtimeHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                regularHours = stdHours.setScale(2, RoundingMode.HALF_UP);
                overtimeHours = totalHours.subtract(stdHours).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            warnings.add("Employee " + empId + " on " + date + ": clock-in >= clock-out; hours set to 0.");
        }

        // Derive attendance status
        String status;
        if (totalHours.compareTo(HALF_HOUR) <= 0) {
            status = "absent";
        } else if (totalHours.compareTo(HALF_DAY_THRESHOLD) < 0) {
            status = "half_day";
        } else {
            status = "present";
        }

        // Upsert AttendanceRecord
        AttendanceRecord ar = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(empId, date)
                .orElseGet(() -> {
                    AttendanceRecord n = new AttendanceRecord();
                    n.setEmployeeId(empId);
                    n.setOrganizationId(organizationId);
                    n.setAttendanceDate(date);
                    return n;
                });

        ar.setClockInTime(clockIn);
        ar.setClockOutTime(clockOut);
        ar.setTotalHours(totalHours);
        ar.setRegularHours(regularHours);
        ar.setOvertimeHours(overtimeHours.compareTo(BigDecimal.ZERO) > 0 ? overtimeHours : BigDecimal.ZERO);
        ar.setStatus(status);
        ar.setWorkLocation("device");
        return attendanceRecordRepository.save(ar);
    }

    private UUID resolveByRawCode(UUID organizationId, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return null;
        // Try matching employee number
        return employeeRepository.findByOrganizationId(organizationId).stream()
                .filter(e -> rawCode.equalsIgnoreCase(e.getEmployeeNumber()))
                .map(Employee::getEmployeeId)
                .findFirst()
                .orElse(null);
    }

    private static String normalizePunchType(String type) {
        if (type == null) return "UNKNOWN";
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "IN", "ENTRY", "CHECK_IN", "CHECKIN" -> "IN";
            case "OUT", "EXIT", "CHECK_OUT", "CHECKOUT" -> "OUT";
            default -> "UNKNOWN";
        };
    }
}
