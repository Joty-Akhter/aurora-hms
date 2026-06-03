package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ImagingAlertRequest;
import com.easyops.hospital.dto.response.ImagingAlertResponse;
import com.easyops.hospital.service.ImagingAlertService;
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

/**
 * Controller for imaging alerts and notifications
 */
@RestController
@RequestMapping("/api/imaging-alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imaging Alerts", description = "APIs for imaging alerts and notifications")
@CrossOrigin(origins = "*")
public class ImagingAlertController {
    
    private final ImagingAlertService alertService;
    
    // ========== Alert Creation ==========
    
    @PostMapping("/critical-finding")
    @Operation(summary = "Create critical finding alert", description = "Create an alert for a critical finding in an imaging study")
    public ResponseEntity<ImagingAlertResponse> createCriticalFindingAlert(
            @Valid @RequestBody ImagingAlertRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating critical finding alert for study: {}", request.getStudyId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingAlertResponse response = alertService.createCriticalFindingAlert(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/abnormal-finding")
    @Operation(summary = "Create abnormal finding alert", description = "Create an alert for an abnormal finding in an imaging study")
    public ResponseEntity<ImagingAlertResponse> createAbnormalFindingAlert(
            @Valid @RequestBody ImagingAlertRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating abnormal finding alert for study: {}", request.getStudyId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingAlertResponse response = alertService.createAbnormalFindingAlert(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/significant-change")
    @Operation(summary = "Create significant change alert", description = "Create an alert for significant changes compared to a prior study")
    public ResponseEntity<ImagingAlertResponse> createSignificantChangeAlert(
            @RequestParam UUID studyId,
            @RequestParam UUID priorStudyId,
            @RequestParam String changeDescription,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating significant change alert for study: {} vs prior: {}", studyId, priorStudyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingAlertResponse response = alertService.createSignificantChangeAlert(studyId, priorStudyId, changeDescription, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/report-available")
    @Operation(summary = "Create report availability notification", description = "Create a notification when an imaging report becomes available")
    public ResponseEntity<ImagingAlertResponse> createReportAvailabilityNotification(
            @RequestParam UUID studyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating report availability notification for study: {}", studyId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingAlertResponse response = alertService.createReportAvailabilityNotification(studyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // ========== Alert Retrieval ==========
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get alerts for patient", description = "Get all imaging alerts for a patient")
    public ResponseEntity<List<ImagingAlertResponse>> getAlertsByPatient(@PathVariable UUID patientId) {
        List<ImagingAlertResponse> alerts = alertService.getAlertsByPatient(patientId);
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/patients/{patientId}/unacknowledged")
    @Operation(summary = "Get unacknowledged alerts", description = "Get unacknowledged imaging alerts for a patient")
    public ResponseEntity<List<ImagingAlertResponse>> getUnacknowledgedAlertsByPatient(@PathVariable UUID patientId) {
        List<ImagingAlertResponse> alerts = alertService.getUnacknowledgedAlertsByPatient(patientId);
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/unacknowledged")
    @Operation(summary = "Get all unacknowledged alerts", description = "Get all unacknowledged imaging alerts")
    public ResponseEntity<List<ImagingAlertResponse>> getAllUnacknowledgedAlerts() {
        List<ImagingAlertResponse> alerts = alertService.getAllUnacknowledgedAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    // ========== Alert Management ==========
    
    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Acknowledge an imaging alert")
    public ResponseEntity<ImagingAlertResponse> acknowledgeAlert(
            @PathVariable UUID alertId,
            @RequestParam(required = false) String acknowledgmentNotes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Acknowledging alert: {}", alertId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingAlertResponse response = alertService.acknowledgeAlert(alertId, acknowledgmentNotes, userId);
        return ResponseEntity.ok(response);
    }
}
