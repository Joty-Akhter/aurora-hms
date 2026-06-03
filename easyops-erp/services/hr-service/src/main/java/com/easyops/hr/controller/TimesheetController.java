package com.easyops.hr.controller;

import com.easyops.hr.entity.Timesheet;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/timesheets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TimesheetController {
    
    private final TimesheetService timesheetService;
    private final HrRbacService hrRbac;
    
    @GetMapping
    public ResponseEntity<List<Timesheet>> getAllTimesheets(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status) {
        
        log.info("GET /timesheets - organizationId: {}, employeeId: {}, status: {}", 
                organizationId, employeeId, status);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<Timesheet> timesheets;
        
        if (status != null && "pending".equals(status)) {
            timesheets = timesheetService.getPendingTimesheets(organizationId);
        } else if (employeeId != null) {
            timesheets = timesheetService.getEmployeeTimesheets(employeeId, organizationId);
        } else {
            timesheets = timesheetService.getAllTimesheets(organizationId);
        }
        
        return ResponseEntity.ok(timesheets);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Timesheet> getTimesheetById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /timesheets/{}", id);
        Timesheet timesheet = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, timesheet.getOrganizationId());
        return ResponseEntity.ok(timesheet);
    }
    
    @PostMapping
    public ResponseEntity<Timesheet> createTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Timesheet timesheet) {
        log.info("POST /timesheets - Creating timesheet for employee: {}", timesheet.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, timesheet.getOrganizationId());
        Timesheet created = timesheetService.createTimesheet(timesheet);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Timesheet> updateTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Timesheet timesheet) {
        log.info("PUT /timesheets/{}", id);
        Timesheet existing = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Timesheet updated = timesheetService.updateTimesheet(id, timesheet);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/submit")
    public ResponseEntity<Timesheet> submitTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("POST /timesheets/{}/submit", id);
        Timesheet existing = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Timesheet submitted = timesheetService.submitTimesheet(id);
        return ResponseEntity.ok(submitted);
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<Timesheet> approveTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID approvedBy = UUID.fromString(request.get("approvedBy"));
        log.info("POST /timesheets/{}/approve - approvedBy: {}", id, approvedBy);
        Timesheet existing = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Timesheet approved = timesheetService.approveTimesheet(id, approvedBy);
        return ResponseEntity.ok(approved);
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Timesheet> rejectTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID rejectedBy = UUID.fromString(request.get("rejectedBy"));
        String rejectionReason = request.get("rejectionReason");
        
        log.info("POST /timesheets/{}/reject - rejectedBy: {}", id, rejectedBy);
        Timesheet existing = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Timesheet rejected = timesheetService.rejectTimesheet(id, rejectedBy, rejectionReason);
        return ResponseEntity.ok(rejected);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimesheet(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("DELETE /timesheets/{}", id);
        Timesheet existing = timesheetService.getTimesheetById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.noContent().build();
    }
}
