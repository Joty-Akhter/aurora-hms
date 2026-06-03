package com.easyops.hr.controller;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.ProvidentFundReportingService;
import com.easyops.hr.service.ProvidentFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/provident-fund/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProvidentFundReportingController {
    
    private final ProvidentFundReportingService reportingService;
    private final ProvidentFundService providentFundService;
    private final HrRbacService hrRbac;
    
    @GetMapping("/executive-dashboard")
    public ResponseEntity<Map<String, Object>> getExecutiveDashboard(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/reports/executive-dashboard - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> dashboard = reportingService.getExecutiveDashboard(organizationId);
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/manager-team")
    public ResponseEntity<Map<String, Object>> getManagerTeamReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID managerId,
            @RequestParam UUID departmentId,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/reports/manager-team - managerId: {}, departmentId: {}", 
                managerId, departmentId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> report = reportingService.getManagerTeamReport(
                managerId, departmentId, organizationId);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/employee-statement")
    public ResponseEntity<Map<String, Object>> getEmployeeStatement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID epfAccountId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("GET /provident-fund/reports/employee-statement - employeeId: {}, accountId: {}", 
                employeeId, epfAccountId);
        
        EpfAccount epfAccount = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, epfAccount.getOrganizationId());
        
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<String, Object> statement = reportingService.getEmployeeStatement(
                employeeId, epfAccountId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }
    
    @GetMapping("/compliance")
    public ResponseEntity<Map<String, Object>> getComplianceReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("GET /provident-fund/reports/compliance - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> report = reportingService.getComplianceReport(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/cost-analysis")
    public ResponseEntity<Map<String, Object>> getCostAnalysisReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/reports/cost-analysis - organizationId: {}, year: {}", 
                organizationId, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> report = reportingService.getCostAnalysisReport(organizationId, year);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/trend-analysis")
    public ResponseEntity<Map<String, Object>> getTrendAnalysisReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "12") Integer months) {
        log.info("GET /provident-fund/reports/trend-analysis - organizationId: {}, months: {}", 
                organizationId, months);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> report = reportingService.getTrendAnalysisReport(organizationId, months);
        return ResponseEntity.ok(report);
    }
}
