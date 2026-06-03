package com.easyops.hospital.controller;

import com.easyops.hospital.dto.response.PatientSummaryResponse;
import com.easyops.hospital.dto.response.PatientTimelineResponse;
import com.easyops.hospital.service.PatientSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Summary & Reporting", description = "APIs for patient summary dashboard, timeline, and reporting")
public class PatientSummaryController {
    
    private final PatientSummaryService patientSummaryService;
    
    @GetMapping
    @Operation(summary = "Get patient summary", description = "Get comprehensive patient summary including active problems, prescriptions, allergies, latest vital signs, and recent notes")
    public ResponseEntity<PatientSummaryResponse> getPatientSummary(@PathVariable UUID patientId) {
        log.info("Getting patient summary for patient: {}", patientId);
        PatientSummaryResponse response = patientSummaryService.getPatientSummary(patientId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/timeline")
    @Operation(summary = "Get patient timeline", description = "Get chronological timeline of patient events (vital signs, notes, prescriptions, problems) within a date range")
    public ResponseEntity<PatientTimelineResponse> getPatientTimeline(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting patient timeline for patient: {} from {} to {}", patientId, startDate, endDate);
        
        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        PatientTimelineResponse response = patientSummaryService.getPatientTimeline(patientId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/export")
    @Operation(summary = "Export patient record", description = "Export comprehensive patient record as JSON for printing or external use")
    public ResponseEntity<PatientSummaryResponse> exportPatientRecord(@PathVariable UUID patientId) {
        log.info("Exporting patient record for patient: {}", patientId);
        PatientSummaryResponse response = patientSummaryService.getPatientSummary(patientId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=patient-record-" + patientId + ".json")
                .body(response);
    }
    
    @GetMapping("/clinical-report")
    @Operation(summary = "Get clinical activity report", description = "Get clinical activity report for a patient including vital signs, notes, and prescriptions activity")
    public ResponseEntity<PatientSummaryResponse> getClinicalReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting clinical report for patient: {} from {} to {}", patientId, startDate, endDate);
        // For now, return summary - can be enhanced with date range filtering
        PatientSummaryResponse response = patientSummaryService.getPatientSummary(patientId);
        return ResponseEntity.ok(response);
    }
}
