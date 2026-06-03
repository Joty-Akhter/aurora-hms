package com.easyops.hr.controller;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.security.HrEmployeeSelfAccessService;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.AdvancedProvidentFundService;
import com.easyops.hr.service.ProvidentFundAnalyticsService;
import com.easyops.hr.service.ProvidentFundComplianceService;
import com.easyops.hr.service.ProvidentFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/provident-fund/advanced")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdvancedProvidentFundController {
    
    private final AdvancedProvidentFundService advancedProvidentFundService;
    private final ProvidentFundComplianceService complianceService;
    private final ProvidentFundAnalyticsService analyticsService;
    private final ProvidentFundService providentFundService;
    private final HrRbacService hrRbac;
    private final HrEmployeeSelfAccessService hrEmployeeSelfAccessService;
    
    // AI Recommendations and Optimization
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/advanced/recommendations - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, organizationId, employeeId);
        Map<String, Object> recommendations = advancedProvidentFundService
                .getProvidentFundRecommendations(employeeId, organizationId);
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeContributions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID epfAccountId) {
        log.info("GET /provident-fund/advanced/optimize - accountId: {}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());
        Map<String, Object> optimization = advancedProvidentFundService.optimizeContributions(epfAccountId);
        return ResponseEntity.ok(optimization);
    }
    
    @GetMapping("/forecast")
    public ResponseEntity<Map<String, Object>> forecastProvidentFund(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID epfAccountId,
            @RequestParam(required = false, defaultValue = "12") Integer months) {
        log.info("GET /provident-fund/advanced/forecast - accountId: {}, months: {}", epfAccountId, months);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());
        Map<String, Object> forecast = advancedProvidentFundService.forecastProvidentFund(epfAccountId, months);
        return ResponseEntity.ok(forecast);
    }
    
    @GetMapping("/risk-assessment")
    public ResponseEntity<Map<String, Object>> assessRisk(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID epfAccountId) {
        log.info("GET /provident-fund/advanced/risk-assessment - accountId: {}", epfAccountId);
        EpfAccount account = providentFundService.getEpfAccountById(epfAccountId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, account.getOrganizationId(), account.getEmployeeId());
        Map<String, Object> riskAssessment = advancedProvidentFundService.assessRisk(epfAccountId);
        return ResponseEntity.ok(riskAssessment);
    }
    
    // Compliance Management
    @GetMapping("/compliance/check")
    public ResponseEntity<Map<String, Object>> checkCompliance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/advanced/compliance/check - organizationId: {}, period: {}/{}", 
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> compliance = complianceService.checkCompliance(organizationId, month, year);
        return ResponseEntity.ok(compliance);
    }
    
    @PostMapping("/compliance/automate")
    public ResponseEntity<Void> automateCompliance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /provident-fund/advanced/compliance/automate - organizationId: {}, period: {}/{}", 
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        complianceService.automateStatutoryCompliance(organizationId, month, year);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/compliance/report")
    public ResponseEntity<Map<String, Object>> generateComplianceReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("GET /provident-fund/advanced/compliance/report - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> report = complianceService.generateComplianceReport(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/compliance/monitor")
    public ResponseEntity<List<Map<String, Object>>> monitorCompliance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/advanced/compliance/monitor - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<Map<String, Object>> alerts = complianceService.monitorCompliance(organizationId);
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/compliance/penalties")
    public ResponseEntity<Map<String, Object>> calculatePenalties(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/advanced/compliance/penalties - organizationId: {}, period: {}/{}", 
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> penalties = complianceService.calculatePenalties(organizationId, month, year);
        return ResponseEntity.ok(penalties);
    }
    
    // Analytics
    @GetMapping("/analytics/participation")
    public ResponseEntity<Map<String, Object>> getParticipationMetrics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/advanced/analytics/participation - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> metrics = analyticsService.getParticipationMetrics(organizationId);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/analytics/contributions")
    public ResponseEntity<Map<String, Object>> analyzeContributions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("GET /provident-fund/advanced/analytics/contributions - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> analysis = analyticsService.analyzeContributions(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(analysis);
    }
    
    @GetMapping("/analytics/costs")
    public ResponseEntity<Map<String, Object>> analyzeCosts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/advanced/analytics/costs - organizationId: {}, year: {}", 
                organizationId, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> costAnalysis = analyticsService.analyzeCosts(organizationId, year);
        return ResponseEntity.ok(costAnalysis);
    }
    
    @GetMapping("/analytics/roi")
    public ResponseEntity<Map<String, Object>> measureROI(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer year) {
        log.info("GET /provident-fund/advanced/analytics/roi - organizationId: {}, year: {}", 
                organizationId, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> roiAnalysis = analyticsService.measureROI(organizationId, year);
        return ResponseEntity.ok(roiAnalysis);
    }
    
    @GetMapping("/analytics/impact")
    public ResponseEntity<Map<String, Object>> analyzeImpact(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /provident-fund/advanced/analytics/impact - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> impact = analyticsService.analyzeImpact(organizationId);
        return ResponseEntity.ok(impact);
    }
}
