package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.VitalSignsRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.service.VitalSignsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/vital-signs")
@RequiredArgsConstructor
@Tag(name = "Vital Signs Management", description = "APIs for managing patient vital signs and clinical measurements")
public class VitalSignsController {
    
    private final VitalSignsService vitalSignsService;
    
    @PostMapping
    @Operation(summary = "Record vital signs", description = "Create a new vital signs record with automatic BMI calculation and abnormal value detection")
    public ResponseEntity<VitalSignsResponse> createVitalSigns(
            @PathVariable UUID patientId,
            @Valid @RequestBody VitalSignsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        VitalSignsResponse response = vitalSignsService.createVitalSigns(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all vital signs for a patient", description = "Retrieve all vital signs records for a patient, ordered by date and time (most recent first)")
    public ResponseEntity<List<VitalSignsResponse>> getVitalSigns(@PathVariable UUID patientId) {
        List<VitalSignsResponse> responses = vitalSignsService.getVitalSignsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{vitalSignId}")
    @Operation(summary = "Get vital signs by ID")
    public ResponseEntity<VitalSignsResponse> getVitalSignsById(@PathVariable UUID vitalSignId) {
        VitalSignsResponse response = vitalSignsService.getVitalSignsById(vitalSignId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/latest")
    @Operation(summary = "Get latest vital signs", description = "Most recent vital signs for the patient, or 204 when none exist")
    public ResponseEntity<VitalSignsResponse> getLatestVitalSigns(@PathVariable UUID patientId) {
        VitalSignsResponse response = vitalSignsService.getLatestVitalSigns(patientId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Get vital signs summary", description = "Get comprehensive vital signs summary including latest, recent, trends, and statistics")
    public ResponseEntity<VitalSignsSummaryResponse> getVitalSignsSummary(@PathVariable UUID patientId) {
        VitalSignsSummaryResponse response = vitalSignsService.getVitalSignsSummary(patientId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/date-range")
    @Operation(summary = "Get vital signs by date range", description = "Retrieve vital signs within a specified date range")
    public ResponseEntity<List<VitalSignsResponse>> getVitalSignsByDateRange(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<VitalSignsResponse> responses = vitalSignsService.getVitalSignsByDateRange(patientId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/abnormal")
    @Operation(summary = "Get abnormal vital signs", description = "Retrieve all vital signs records with abnormal values")
    public ResponseEntity<List<VitalSignsResponse>> getAbnormalVitalSigns(@PathVariable UUID patientId) {
        List<VitalSignsResponse> responses = vitalSignsService.getAbnormalVitalSigns(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/critical")
    @Operation(summary = "Get critical vital signs", description = "Retrieve all vital signs records with critical values requiring immediate attention")
    public ResponseEntity<List<VitalSignsResponse>> getCriticalVitalSigns(@PathVariable UUID patientId) {
        List<VitalSignsResponse> responses = vitalSignsService.getCriticalVitalSigns(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/trends")
    @Operation(summary = "Get vital signs trends", description = "Retrieve daily aggregated vital signs trends for analytics and visualization")
    public ResponseEntity<List<VitalSignsTrendResponse>> getVitalSignsTrends(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30); // Default to last 30 days
        }
        List<VitalSignsTrendResponse> responses = vitalSignsService.getVitalSignsTrends(patientId, startDate);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/encounter/{encounterId}")
    @Operation(summary = "Get vital signs by encounter", description = "Retrieve all vital signs recorded during a specific encounter")
    public ResponseEntity<List<VitalSignsResponse>> getVitalSignsByEncounter(@PathVariable UUID encounterId) {
        List<VitalSignsResponse> responses = vitalSignsService.getVitalSignsByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{vitalSignId}")
    @Operation(summary = "Update vital signs", description = "Update an existing vital signs record with automatic BMI recalculation and abnormal value recheck")
    public ResponseEntity<VitalSignsResponse> updateVitalSigns(
            @PathVariable UUID patientId,
            @PathVariable UUID vitalSignId,
            @Valid @RequestBody VitalSignsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        VitalSignsResponse response = vitalSignsService.updateVitalSigns(vitalSignId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{vitalSignId}")
    @Operation(summary = "Delete vital signs")
    public ResponseEntity<Void> deleteVitalSigns(@PathVariable UUID vitalSignId) {
        vitalSignsService.deleteVitalSigns(vitalSignId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/calculate-bmi")
    @Operation(summary = "Calculate BMI", description = "Calculate Body Mass Index from weight and height")
    public ResponseEntity<BigDecimal> calculateBmi(
            @RequestParam BigDecimal weight,
            @RequestParam String weightUnit,
            @RequestParam BigDecimal height,
            @RequestParam String heightUnit) {
        com.easyops.hospital.entity.VitalSigns.WeightUnit wUnit = 
            com.easyops.hospital.entity.VitalSigns.WeightUnit.valueOf(weightUnit);
        com.easyops.hospital.entity.VitalSigns.HeightUnit hUnit = 
            com.easyops.hospital.entity.VitalSigns.HeightUnit.valueOf(heightUnit);
        BigDecimal bmi = vitalSignsService.calculateBmi(weight, wUnit, height, hUnit);
        return ResponseEntity.ok(bmi);
    }
}
