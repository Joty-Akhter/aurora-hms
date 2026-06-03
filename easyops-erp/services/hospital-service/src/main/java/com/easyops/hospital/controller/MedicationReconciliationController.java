package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.MedicationReconciliationRequest;
import com.easyops.hospital.dto.response.MedicationReconciliationResponse;
import com.easyops.hospital.service.MedicationReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medication-reconciliation")
@RequiredArgsConstructor
@Slf4j
public class MedicationReconciliationController {
    
    private final MedicationReconciliationService reconciliationService;
    
    /**
     * Create a new medication reconciliation
     */
    @PostMapping
    public ResponseEntity<MedicationReconciliationResponse> createReconciliation(
            @Valid @RequestBody MedicationReconciliationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating medication reconciliation for patient: {}", request.getPatientId());
        MedicationReconciliationResponse response = reconciliationService.createReconciliation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get reconciliation by ID
     */
    @GetMapping("/{reconciliationId}")
    public ResponseEntity<MedicationReconciliationResponse> getReconciliationById(
            @PathVariable UUID reconciliationId) {
        MedicationReconciliationResponse response = reconciliationService.getReconciliationById(reconciliationId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all reconciliations for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicationReconciliationResponse>> getReconciliationsByPatient(
            @PathVariable UUID patientId) {
        List<MedicationReconciliationResponse> reconciliations = 
            reconciliationService.getReconciliationsByPatient(patientId);
        return ResponseEntity.ok(reconciliations);
    }
    
    /**
     * Complete reconciliation
     */
    @PostMapping("/{reconciliationId}/complete")
    public ResponseEntity<MedicationReconciliationResponse> completeReconciliation(
            @PathVariable UUID reconciliationId,
            @RequestParam UUID verifiedBy,
            @RequestParam String verifiedByName,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Completing reconciliation: {}", reconciliationId);
        MedicationReconciliationResponse response = 
            reconciliationService.completeReconciliation(reconciliationId, verifiedBy, verifiedByName, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Apply reconciliation actions to medications
     */
    @PostMapping("/{reconciliationId}/apply")
    public ResponseEntity<MedicationReconciliationResponse> applyReconciliation(
            @PathVariable UUID reconciliationId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Applying reconciliation: {}", reconciliationId);
        MedicationReconciliationResponse response = reconciliationService.applyReconciliation(reconciliationId, userId);
        return ResponseEntity.ok(response);
    }
}
