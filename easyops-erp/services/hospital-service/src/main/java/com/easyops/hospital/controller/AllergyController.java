package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.AllergyRequest;
import com.easyops.hospital.dto.response.AllergyResponse;
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
@RequestMapping("/api/patients/{patientId}/allergies")
@RequiredArgsConstructor
@Tag(name = "Allergy Management", description = "APIs for managing patient allergies and adverse reactions")
public class AllergyController {
    
    private final MedicalHistoryService medicalHistoryService;
    
    @PostMapping
    @Operation(summary = "Add allergy to patient", description = "Creates a new allergy record. Prevents duplicate allergies.")
    public ResponseEntity<AllergyResponse> createAllergy(
            @PathVariable UUID patientId,
            @Valid @RequestBody AllergyRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        AllergyResponse response = medicalHistoryService.createAllergy(patientId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all allergies for a patient")
    public ResponseEntity<List<AllergyResponse>> getAllergies(@PathVariable UUID patientId) {
        List<AllergyResponse> responses = medicalHistoryService.getAllergiesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active allergies for a patient")
    public ResponseEntity<List<AllergyResponse>> getActiveAllergies(@PathVariable UUID patientId) {
        List<AllergyResponse> responses = medicalHistoryService.getActiveAllergiesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{allergyId}")
    @Operation(summary = "Update allergy")
    public ResponseEntity<AllergyResponse> updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @Valid @RequestBody AllergyRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        AllergyResponse response = medicalHistoryService.updateAllergy(allergyId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{allergyId}")
    @Operation(summary = "Delete allergy")
    public ResponseEntity<Void> deleteAllergy(@PathVariable UUID allergyId) {
        medicalHistoryService.deleteAllergy(allergyId);
        return ResponseEntity.noContent().build();
    }
}
