package com.easyops.hospital.controller;

import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.service.MedicationReportingService;
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
@RequestMapping("/api/medications/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Medication Reports", description = "Medication reporting and analytics APIs")
@CrossOrigin(origins = "*")
public class MedicationReportingController {
    
    private final MedicationReportingService reportingService;
    
    // ========== Medication List Reports ==========
    
    /**
     * Generate complete medication list report (current + historical)
     */
    @GetMapping("/patient/{patientId}/list/complete")
    @Operation(summary = "Generate complete medication list report")
    public ResponseEntity<MedicationListReportResponse> generateCompleteMedicationListReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/list/complete", patientId);
        MedicationListReportResponse report = reportingService.generateCompleteMedicationListReport(patientId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate current medication list report
     */
    @GetMapping("/patient/{patientId}/list/current")
    @Operation(summary = "Generate current medication list report")
    public ResponseEntity<MedicationListReportResponse> generateCurrentMedicationListReport(
            @PathVariable UUID patientId) {
        log.info("GET /api/medications/reports/patient/{}/list/current", patientId);
        MedicationListReportResponse report = reportingService.generateCurrentMedicationListReport(patientId);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate historical medication list report
     */
    @GetMapping("/patient/{patientId}/list/historical")
    @Operation(summary = "Generate historical medication list report")
    public ResponseEntity<MedicationListReportResponse> generateHistoricalMedicationListReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/list/historical", patientId);
        MedicationListReportResponse report = reportingService.generateHistoricalMedicationListReport(patientId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    // ========== Medications by Indication Reports ==========
    
    /**
     * Generate medications by indication report
     */
    @GetMapping("/patient/{patientId}/by-indication")
    @Operation(summary = "Generate medications by indication report")
    public ResponseEntity<MedicationIndicationReportResponse> generateMedicationsByIndicationReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) String indication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/by-indication", patientId);
        MedicationIndicationReportResponse report = reportingService.generateMedicationsByIndicationReport(patientId, indication, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    // ========== Medication Adherence Reports ==========
    
    /**
     * Generate medication adherence report
     */
    @GetMapping("/patient/{patientId}/adherence")
    @Operation(summary = "Generate medication adherence report")
    public ResponseEntity<MedicationAdherenceReportResponse> generateMedicationAdherenceReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/adherence", patientId);
        MedicationAdherenceReportResponse report = reportingService.generateMedicationAdherenceReport(patientId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    // ========== Medication List Completeness Metrics ==========
    
    /**
     * Generate medication list completeness metrics
     */
    @GetMapping("/patient/{patientId}/completeness")
    @Operation(summary = "Generate medication list completeness metrics")
    public ResponseEntity<MedicationCompletenessMetricsResponse> generateMedicationCompletenessMetrics(
            @PathVariable UUID patientId) {
        log.info("GET /api/medications/reports/patient/{}/completeness", patientId);
        MedicationCompletenessMetricsResponse metrics = reportingService.generateMedicationCompletenessMetrics(patientId);
        return ResponseEntity.ok(metrics);
    }
    
    // ========== Clinical Reports ==========
    
    /**
     * Generate medications by provider report
     */
    @GetMapping("/patient/{patientId}/clinical/by-provider")
    @Operation(summary = "Generate medications by provider report")
    public ResponseEntity<MedicationClinicalReportResponse> generateMedicationsByProviderReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/clinical/by-provider", patientId);
        MedicationClinicalReportResponse report = reportingService.generateMedicationsByProviderReport(patientId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate medications by problem report
     */
    @GetMapping("/patient/{patientId}/clinical/by-problem")
    @Operation(summary = "Generate medications by problem report")
    public ResponseEntity<MedicationClinicalReportResponse> generateMedicationsByProblemReport(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/clinical/by-problem", patientId);
        MedicationClinicalReportResponse report = reportingService.generateMedicationsByProblemReport(patientId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    // ========== Quality Metrics ==========
    
    /**
     * Generate medication quality metrics report
     */
    @GetMapping("/patient/{patientId}/quality")
    @Operation(summary = "Generate medication quality metrics report")
    public ResponseEntity<MedicationQualityMetricsResponse> generateMedicationQualityMetrics(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/medications/reports/patient/{}/quality", patientId);
        MedicationQualityMetricsResponse metrics = reportingService.generateMedicationQualityMetrics(patientId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
}
