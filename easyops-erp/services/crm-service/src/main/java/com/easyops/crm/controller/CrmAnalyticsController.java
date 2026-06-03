package com.easyops.crm.controller;

import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.CrmAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class CrmAnalyticsController {

    @Autowired
    private CrmAnalyticsService analyticsService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getOverallDashboard(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> stats = analyticsService.getOverallDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/leads")
    public ResponseEntity<Map<String, Object>> getLeadAnalytics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> analytics = analyticsService.getLeadAnalytics(organizationId);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/opportunities")
    public ResponseEntity<Map<String, Object>> getOpportunityAnalytics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> analytics = analyticsService.getOpportunityAnalytics(organizationId);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/support")
    public ResponseEntity<Map<String, Object>> getSupportAnalytics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> analytics = analyticsService.getSupportAnalytics(organizationId);
        return ResponseEntity.ok(analytics);
    }
}
