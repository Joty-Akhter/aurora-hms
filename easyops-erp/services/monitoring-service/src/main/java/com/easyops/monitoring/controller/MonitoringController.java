package com.easyops.monitoring.controller;

import com.easyops.monitoring.dto.AlertResponse;
import com.easyops.monitoring.dto.ServiceHealthResponse;
import com.easyops.monitoring.dto.SystemMetrics;
import com.easyops.monitoring.security.MonitoringRbacService;
import com.easyops.monitoring.security.RbacRequestHeaders;
import com.easyops.monitoring.service.AlertService;
import com.easyops.monitoring.service.HealthCheckService;
import com.easyops.monitoring.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "System monitoring and health checks")
public class MonitoringController {
    
    private final HealthCheckService healthCheckService;
    private final AlertService alertService;
    private final MetricsService metricsService;
    private final MonitoringRbacService monitoringRbac;
    
    @GetMapping("/services")
    @Operation(summary = "Get all services health status")
    public ResponseEntity<List<ServiceHealthResponse>> getAllServicesHealth(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(healthCheckService.getAllServicesHealth());
    }
    
    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get specific service health")
    public ResponseEntity<ServiceHealthResponse> getServiceHealth(
            @PathVariable String serviceName,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(healthCheckService.getServiceHealth(serviceName));
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics summary")
    public ResponseEntity<SystemMetrics> getSystemMetrics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(metricsService.getSystemMetrics());
    }
    
    @GetMapping("/alerts")
    @Operation(summary = "Get active alerts")
    public ResponseEntity<List<AlertResponse>> getActiveAlerts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }
    
    @GetMapping("/alerts/service/{serviceName}")
    @Operation(summary = "Get alerts for specific service")
    public ResponseEntity<Page<AlertResponse>> getServiceAlerts(
            @PathVariable String serviceName,
            Pageable pageable,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(alertService.getAlertsByService(serviceName, pageable));
    }
    
    @PostMapping("/alerts/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemConfigure(actor, organizationId);
        alertService.acknowledgeAlert(id, actor);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/alerts/{id}/resolve")
    @Operation(summary = "Resolve an alert")
    public ResponseEntity<Void> resolveAlert(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemConfigure(actor, organizationId);
        alertService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/health-check")
    @Operation(summary = "Trigger manual health check")
    public ResponseEntity<String> triggerHealthCheck(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        monitoringRbac.requireSystemConfigure(actor, organizationId);
        healthCheckService.performHealthChecks();
        return ResponseEntity.ok("Health check triggered");
    }
}
