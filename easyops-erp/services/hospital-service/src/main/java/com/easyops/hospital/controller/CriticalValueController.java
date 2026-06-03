package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.CriticalValueAcknowledgmentRequest;
import com.easyops.hospital.dto.request.CriticalValueEscalationRequest;
import com.easyops.hospital.dto.response.CriticalValueAlertResponse;
import com.easyops.hospital.dto.response.CriticalValueDocumentationResponse;
import com.easyops.hospital.service.CriticalValueManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/critical-values")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Critical Value Management", description = "APIs for critical value detection, alerts, acknowledgment, documentation, and escalation")
public class CriticalValueController {
    
    private final CriticalValueManagementService criticalValueManagementService;
    
    // ========== Critical Value Alerts ==========
    
    @GetMapping("/alerts")
    @Operation(summary = "Get all critical value alerts", 
               description = "Retrieve all critical value alerts in the system")
    public ResponseEntity<List<CriticalValueAlertResponse>> getAllAlerts() {
        List<CriticalValueAlertResponse> alerts = criticalValueManagementService.getAllCriticalValueAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/alerts/unacknowledged")
    @Operation(summary = "Get unacknowledged alerts", 
               description = "Retrieve all unacknowledged critical value alerts")
    public ResponseEntity<List<CriticalValueAlertResponse>> getUnacknowledgedAlerts() {
        List<CriticalValueAlertResponse> alerts = criticalValueManagementService.getUnacknowledgedAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/alerts/{alertId}")
    @Operation(summary = "Get alert by ID", 
               description = "Retrieve a specific critical value alert by its ID")
    public ResponseEntity<CriticalValueAlertResponse> getAlertById(@PathVariable UUID alertId) {
        CriticalValueAlertResponse alert = criticalValueManagementService.getAlertById(alertId);
        return ResponseEntity.ok(alert);
    }
    
    @GetMapping("/alerts/patients/{patientId}")
    @Operation(summary = "Get alerts by patient", 
               description = "Retrieve all critical value alerts for a specific patient")
    public ResponseEntity<List<CriticalValueAlertResponse>> getAlertsByPatient(@PathVariable UUID patientId) {
        List<CriticalValueAlertResponse> alerts = criticalValueManagementService.getAlertsByPatient(patientId);
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/alerts/providers/{providerId}")
    @Operation(summary = "Get alerts by provider", 
               description = "Retrieve all unacknowledged critical value alerts for a specific provider")
    public ResponseEntity<List<CriticalValueAlertResponse>> getAlertsByProvider(@PathVariable UUID providerId) {
        List<CriticalValueAlertResponse> alerts = criticalValueManagementService.getAlertsByProvider(providerId);
        return ResponseEntity.ok(alerts);
    }
    
    // ========== Critical Value Acknowledgment ==========
    
    @PostMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge critical value", 
               description = "Acknowledge a critical value alert with provider response")
    public ResponseEntity<CriticalValueAlertResponse> acknowledgeCriticalValue(
            @PathVariable UUID alertId,
            @Valid @RequestBody CriticalValueAcknowledgmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Acknowledging critical value alert: {}", alertId);
        if (userId == null) userId = UUID.randomUUID();
        
        com.easyops.hospital.entity.LabCriticalValueAlert alertEntity = criticalValueManagementService
            .acknowledgeCriticalValue(alertId, request, userId);
        CriticalValueAlertResponse alert = criticalValueManagementService.getAlertById(alertEntity.getAlertId());
        return ResponseEntity.ok(alert);
    }
    
    // ========== Critical Value Escalation ==========
    
    @PostMapping("/alerts/{alertId}/escalate")
    @Operation(summary = "Escalate critical value", 
               description = "Escalate a critical value alert to another provider")
    public ResponseEntity<CriticalValueAlertResponse> escalateCriticalValue(
            @PathVariable UUID alertId,
            @Valid @RequestBody CriticalValueEscalationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Escalating critical value alert: {}", alertId);
        if (userId == null) userId = UUID.randomUUID();
        
        com.easyops.hospital.entity.LabCriticalValueAlert alertEntity = criticalValueManagementService
            .escalateCriticalValue(alertId, request, userId);
        CriticalValueAlertResponse alert = criticalValueManagementService.getAlertById(alertEntity.getAlertId());
        return ResponseEntity.ok(alert);
    }
    
    @PostMapping("/alerts/check-escalation")
    @Operation(summary = "Check and escalate unacknowledged alerts", 
               description = "Automatically check and escalate unacknowledged alerts that have exceeded timeout")
    public ResponseEntity<Void> checkAndEscalateUnacknowledgedAlerts() {
        log.info("Checking for unacknowledged alerts to escalate");
        criticalValueManagementService.checkAndEscalateUnacknowledgedAlerts();
        return ResponseEntity.ok().build();
    }
    
    // ========== Critical Value Documentation ==========
    
    @GetMapping("/results/{resultId}/documentation")
    @Operation(summary = "Get critical value documentation", 
               description = "Retrieve complete documentation for a critical value result")
    public ResponseEntity<CriticalValueDocumentationResponse> getCriticalValueDocumentation(
            @PathVariable UUID resultId) {
        CriticalValueDocumentationResponse documentation = 
            criticalValueManagementService.getCriticalValueDocumentation(resultId);
        return ResponseEntity.ok(documentation);
    }
}
