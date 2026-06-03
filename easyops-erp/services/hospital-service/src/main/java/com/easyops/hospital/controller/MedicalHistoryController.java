package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.MedicalHistoryRequest;
import com.easyops.hospital.dto.response.MedicalHistoryResponse;
import com.easyops.hospital.service.MedicalHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/medical-history")
@RequiredArgsConstructor
@Tag(name = "Medical History Management", description = "APIs for managing patient medical history")
public class MedicalHistoryController {
    
    private final MedicalHistoryService medicalHistoryService;
    
    @PostMapping
    @Operation(summary = "Add medical history to patient")
    public ResponseEntity<MedicalHistoryResponse> createMedicalHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody MedicalHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        MedicalHistoryResponse response = medicalHistoryService.createMedicalHistory(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all medical history for a patient")
    public ResponseEntity<List<MedicalHistoryResponse>> getMedicalHistory(@PathVariable UUID patientId) {
        List<MedicalHistoryResponse> responses = medicalHistoryService.getMedicalHistoryByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/past-medical")
    @Operation(summary = "Get past medical history for a patient")
    public ResponseEntity<List<MedicalHistoryResponse>> getPastMedicalHistory(@PathVariable UUID patientId) {
        List<MedicalHistoryResponse> responses = medicalHistoryService.getPastMedicalHistoryByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{historyId}")
    @Operation(summary = "Update medical history")
    public ResponseEntity<MedicalHistoryResponse> updateMedicalHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID historyId,
            @Valid @RequestBody MedicalHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        MedicalHistoryResponse response = medicalHistoryService.updateMedicalHistory(historyId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{historyId}")
    @Operation(summary = "Delete medical history")
    public ResponseEntity<Void> deleteMedicalHistory(@PathVariable UUID historyId) {
        medicalHistoryService.deleteMedicalHistory(historyId);
        return ResponseEntity.noContent().build();
    }
}
