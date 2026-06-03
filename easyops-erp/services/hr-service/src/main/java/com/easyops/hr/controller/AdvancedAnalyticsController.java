package com.easyops.hr.controller;

import com.easyops.hr.service.AdvancedAnalyticsService;
import com.easyops.hr.service.CustomReportBuilderService;
import com.easyops.hr.service.ScheduledReportingService;
import com.easyops.hr.entity.ScheduledReport;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvancedAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AdvancedAnalyticsController.class);
    
    private final AdvancedAnalyticsService analyticsService;
    private final CustomReportBuilderService customReportBuilderService;
    private final ScheduledReportingService scheduledReportingService;
    private final HrRbacService hrRbac;
    
    @GetMapping("/forecast/provident-fund-participation")
    public ResponseEntity<Map<String, Object>> forecastProvidentFundParticipation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "12") Integer months) {
        log.info("GET /analytics/forecast/provident-fund-participation - organizationId: {}, months: {}", 
                organizationId, months);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> forecast = analyticsService.forecastProvidentFundParticipation(organizationId, months);
        return ResponseEntity.ok(forecast);
    }
    
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> analyzeTrendsAndPatterns(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam String entityType,
            @RequestParam(required = false, defaultValue = "12") Integer months) {
        log.info("GET /analytics/trends - organizationId: {}, type: {}, months: {}", 
                organizationId, entityType, months);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> analysis = analyticsService.analyzeTrendsAndPatterns(
                organizationId, entityType, months);
        return ResponseEntity.ok(analysis);
    }
    
    // Custom Report Builder
    @PostMapping("/custom-report")
    public ResponseEntity<Map<String, Object>> buildCustomReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> reportConfig) {
        log.info("POST /analytics/custom-report - Building custom report");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, null);
        Map<String, Object> report = customReportBuilderService.buildCustomReport(reportConfig);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/custom-report/types")
    public ResponseEntity<List<Map<String, Object>>> getAvailableReportTypes(
            @RequestHeader("X-User-Id") String userIdHeader) {
        log.info("GET /analytics/custom-report/types");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, null);
        List<Map<String, Object>> types = customReportBuilderService.getAvailableReportTypes();
        return ResponseEntity.ok(types);
    }
    
    // Scheduled Reporting
    @PostMapping("/scheduled-reports")
    public ResponseEntity<ScheduledReport> createScheduledReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ScheduledReport scheduledReport) {
        log.info("POST /analytics/scheduled-reports - Creating scheduled report");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, scheduledReport.getOrganizationId());
        ScheduledReport created = scheduledReportingService.createScheduledReport(scheduledReport);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/scheduled-reports")
    public ResponseEntity<List<ScheduledReport>> getScheduledReports(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /analytics/scheduled-reports - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<ScheduledReport> reports = scheduledReportingService.getScheduledReports(organizationId);
        return ResponseEntity.ok(reports);
    }
    
    @PutMapping("/scheduled-reports/{id}")
    public ResponseEntity<ScheduledReport> updateScheduledReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody ScheduledReport reportData) {
        log.info("PUT /analytics/scheduled-reports/{}", id);
        ScheduledReport existing = scheduledReportingService.getScheduledReportById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        ScheduledReport updated = scheduledReportingService.updateScheduledReport(id, reportData);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/scheduled-reports/{id}/execute")
    public ResponseEntity<Void> executeScheduledReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("POST /analytics/scheduled-reports/{}/execute", id);
        ScheduledReport report = scheduledReportingService.getScheduledReportById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, report.getOrganizationId());
        scheduledReportingService.executeReport(report);
        return ResponseEntity.ok().build();
    }
}
