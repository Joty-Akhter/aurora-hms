package com.easyops.hr.controller;

import com.easyops.hr.entity.Holiday;
import com.easyops.hr.repository.HolidayRepository;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/holidays")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HolidayController {

    private final HolidayRepository holidayRepository;
    private final HrRbacService hrRbac;

    @GetMapping
    public ResponseEntity<List<Holiday>> getHolidays(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<Holiday> holidays;
        if (startDate != null && endDate != null) {
            holidays = holidayRepository.findHolidaysInRange(organizationId, startDate, endDate);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            holidays = holidayRepository.findByOrganizationIdAndIsActive(organizationId, true);
        } else {
            holidays = holidayRepository.findByOrganizationId(organizationId);
        }
        return ResponseEntity.ok(holidays);
    }

    @PostMapping
    public ResponseEntity<Holiday> createHoliday(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Holiday holiday) {
        if (holiday.getOrganizationId() == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, holiday.getOrganizationId());
        log.info("POST /holidays - Creating: {}", holiday.getHolidayName());
        holiday.setHolidayId(null);
        holiday.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayRepository.save(holiday));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Holiday> updateHoliday(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Holiday holiday) {
        Holiday existing = holidayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "holiday_not_found"));
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        if (holiday.getHolidayName() != null) existing.setHolidayName(holiday.getHolidayName());
        if (holiday.getHolidayDate() != null) existing.setHolidayDate(holiday.getHolidayDate());
        if (holiday.getHolidayType() != null) existing.setHolidayType(holiday.getHolidayType());
        if (holiday.getDescription() != null) existing.setDescription(holiday.getDescription());
        if (holiday.getIsRecurring() != null) existing.setIsRecurring(holiday.getIsRecurring());
        if (holiday.getIsActive() != null) existing.setIsActive(holiday.getIsActive());
        existing.setDepartmentId(holiday.getDepartmentId());
        existing.setEmployeeId(holiday.getEmployeeId());
        existing.setUpdatedAt(LocalDateTime.now());
        log.info("PUT /holidays/{} - Updating: {}", id, existing.getHolidayName());
        return ResponseEntity.ok(holidayRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        Holiday h = holidayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "holiday_not_found"));
        if (!h.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "holiday_org_mismatch");
        }
        log.info("DELETE /holidays/{}", id);
        holidayRepository.delete(h);
    }
}
