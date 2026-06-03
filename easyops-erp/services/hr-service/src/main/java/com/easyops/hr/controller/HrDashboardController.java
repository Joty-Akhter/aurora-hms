package com.easyops.hr.controller;

import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeService;
import com.easyops.hr.service.HrDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HrDashboardController {
    
    private final HrDashboardService dashboardService;
    private final EmployeeService employeeService;
    private final HrRbacService hrRbac;
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /dashboard/stats - organizationId: {}", organizationId);
        Map<String, Object> stats = dashboardService.getDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/headcount-by-department")
    public ResponseEntity<List<Map<String, Object>>> getHeadcountByDepartment(@RequestParam UUID organizationId) {
        log.info("GET /dashboard/headcount-by-department - organizationId: {}", organizationId);
        List<Map<String, Object>> headcount = dashboardService.getHeadcountByDepartment(organizationId);
        return ResponseEntity.ok(headcount);
    }
    
    @GetMapping("/recent-hires")
    public ResponseEntity<List<Map<String, Object>>> getRecentHires(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "5") int limit) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /dashboard/recent-hires - organizationId: {}, limit: {}", organizationId, limit);
        List<Map<String, Object>> recentHires = dashboardService.getRecentHires(organizationId, limit);
        return ResponseEntity.ok(recentHires);
    }
    
    @GetMapping("/employee-summary/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID employeeId) {
        log.info("GET /dashboard/employee-summary/{}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, employeeService.getEmployeeById(employeeId).getOrganizationId());
        Map<String, Object> summary = dashboardService.getEmployeeSummary(employeeId);
        return ResponseEntity.ok(summary);
    }
}

