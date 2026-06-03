package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ImmunizationRequest;
import com.easyops.hospital.dto.response.ImmunizationResponse;
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
@RequestMapping("/api/patients/{patientId}/immunizations")
@RequiredArgsConstructor
@Tag(name = "Immunization Management", description = "APIs for managing patient immunizations")
public class ImmunizationController {
    
    private final MedicalHistoryService medicalHistoryService;
    
    @PostMapping
    @Operation(summary = "Add immunization to patient")
    public ResponseEntity<ImmunizationResponse> createImmunization(
            @PathVariable UUID patientId,
            @Valid @RequestBody ImmunizationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        ImmunizationResponse response = medicalHistoryService.createImmunization(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all immunizations for a patient")
    public ResponseEntity<List<ImmunizationResponse>> getImmunizations(@PathVariable UUID patientId) {
        List<ImmunizationResponse> responses = medicalHistoryService.getImmunizationsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{immunizationId}")
    @Operation(summary = "Update immunization")
    public ResponseEntity<ImmunizationResponse> updateImmunization(
            @PathVariable UUID patientId,
            @PathVariable UUID immunizationId,
            @Valid @RequestBody ImmunizationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        ImmunizationResponse response = medicalHistoryService.updateImmunization(immunizationId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{immunizationId}")
    @Operation(summary = "Delete immunization")
    public ResponseEntity<Void> deleteImmunization(@PathVariable UUID immunizationId) {
        medicalHistoryService.deleteImmunization(immunizationId);
        return ResponseEntity.noContent().build();
    }
}
