package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.FamilyHistoryRequest;
import com.easyops.hospital.dto.response.FamilyHistoryResponse;
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
@RequestMapping("/api/patients/{patientId}/family-history")
@RequiredArgsConstructor
@Tag(name = "Family History Management", description = "APIs for managing patient family history")
public class FamilyHistoryController {
    
    private final MedicalHistoryService medicalHistoryService;
    
    @PostMapping
    @Operation(summary = "Add family history to patient")
    public ResponseEntity<FamilyHistoryResponse> createFamilyHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody FamilyHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        FamilyHistoryResponse response = medicalHistoryService.createFamilyHistory(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all family history for a patient")
    public ResponseEntity<List<FamilyHistoryResponse>> getFamilyHistory(@PathVariable UUID patientId) {
        List<FamilyHistoryResponse> responses = medicalHistoryService.getFamilyHistoryByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{familyHistoryId}")
    @Operation(summary = "Update family history")
    public ResponseEntity<FamilyHistoryResponse> updateFamilyHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID familyHistoryId,
            @Valid @RequestBody FamilyHistoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        FamilyHistoryResponse response = medicalHistoryService.updateFamilyHistory(familyHistoryId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{familyHistoryId}")
    @Operation(summary = "Delete family history")
    public ResponseEntity<Void> deleteFamilyHistory(@PathVariable UUID familyHistoryId) {
        medicalHistoryService.deleteFamilyHistory(familyHistoryId);
        return ResponseEntity.noContent().build();
    }
}
