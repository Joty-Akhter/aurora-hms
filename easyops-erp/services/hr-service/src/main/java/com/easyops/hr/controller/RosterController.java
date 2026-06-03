package com.easyops.hr.controller;

import com.easyops.hr.dto.roster.RosterMonthViewDto;
import com.easyops.hr.entity.ShiftSchedule;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/roster")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RosterController {

    private final RosterService rosterService;
    private final HrRbacService hrRbac;

    @GetMapping("/month-view")
    public ResponseEntity<RosterMonthViewDto> monthView(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID departmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(rosterService.buildMonthView(organizationId, year, month, departmentId));
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<ShiftSchedule>> listSchedules(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) UUID departmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(rosterService.listSchedules(organizationId, startDate, endDate, departmentId));
    }

    @PostMapping("/schedules")
    public ResponseEntity<ShiftSchedule> createSchedule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ShiftSchedule body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, body.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(rosterService.createSchedule(body));
    }

    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ShiftSchedule> updateSchedule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID scheduleId,
            @RequestBody ShiftSchedule body) {
        ShiftSchedule existing = rosterService.getSchedule(scheduleId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(rosterService.updateSchedule(scheduleId, body));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID scheduleId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        rosterService.deleteSchedule(scheduleId, organizationId);
        return ResponseEntity.noContent().build();
    }
}
