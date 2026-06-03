package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.EncounterRequest;
import com.easyops.hospital.dto.response.EncounterResponse;
import com.easyops.hospital.entity.Encounter;
import com.easyops.hospital.service.EncounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/encounters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Encounter Management", description = "APIs for managing patient encounters and visits")
public class EncounterController {
    
    private final EncounterService encounterService;
    
    @PostMapping
    @Operation(summary = "Create new encounter", description = "Create a new patient encounter/visit with automatic encounter number generation")
    public ResponseEntity<EncounterResponse> createEncounter(
            @RequestParam UUID organizationId,
            @Valid @RequestBody EncounterRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating encounter for patient: {} in organization: {}", request.getPatientId(), organizationId);
        EncounterResponse response = encounterService.createEncounter(organizationId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{encounterId}")
    @Operation(summary = "Get encounter by ID")
    public ResponseEntity<EncounterResponse> getEncounterById(@PathVariable UUID encounterId) {
        log.info("Getting encounter: {}", encounterId);
        EncounterResponse response = encounterService.getEncounterById(encounterId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{encounterNumber}")
    @Operation(summary = "Get encounter by encounter number")
    public ResponseEntity<EncounterResponse> getEncounterByNumber(@PathVariable String encounterNumber) {
        log.info("Getting encounter by number: {}", encounterNumber);
        EncounterResponse response = encounterService.getEncounterByNumber(encounterNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all encounters for a patient", description = "Retrieve all encounters for a patient, ordered by date (most recent first)")
    public ResponseEntity<List<EncounterResponse>> getEncountersByPatient(@PathVariable UUID patientId) {
        log.info("Getting encounters for patient: {}", patientId);
        List<EncounterResponse> responses = encounterService.getEncountersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patient/{patientId}/active")
    @Operation(summary = "Get active encounters for a patient", description = "Retrieve all active (in-progress, arrived, admitted) encounters for a patient")
    public ResponseEntity<List<EncounterResponse>> getActiveEncountersByPatient(@PathVariable UUID patientId) {
        log.info("Getting active encounters for patient: {}", patientId);
        List<EncounterResponse> responses = encounterService.getActiveEncountersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patient/{patientId}/admissions/active")
    @Operation(summary = "Get active admissions for a patient", description = "Retrieve all active hospital admissions for a patient")
    public ResponseEntity<List<EncounterResponse>> getActiveAdmissionsByPatient(@PathVariable UUID patientId) {
        log.info("Getting active admissions for patient: {}", patientId);
        List<EncounterResponse> responses = encounterService.getActiveAdmissionsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping
    @Operation(summary = "Get encounters by organization", description = "Retrieve all encounters for an organization with optional filters")
    public ResponseEntity<List<EncounterResponse>> getEncounters(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String encounterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting encounters for organization: {}, status: {}, type: {}", organizationId, status, encounterType);
        
        List<EncounterResponse> responses;
        
        if (status != null) {
            Encounter.EncounterStatus statusEnum = Encounter.EncounterStatus.valueOf(status);
            responses = encounterService.getEncountersByStatus(organizationId, statusEnum);
        } else if (encounterType != null) {
            Encounter.EncounterType typeEnum = Encounter.EncounterType.valueOf(encounterType);
            responses = encounterService.getEncountersByType(organizationId, typeEnum);
        } else if (startDate != null && endDate != null) {
            responses = encounterService.getEncountersByDateRange(organizationId, startDate, endDate);
        } else {
            responses = encounterService.getEncountersByOrganization(organizationId);
        }
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active encounters", description = "Retrieve all active encounters for an organization")
    public ResponseEntity<List<EncounterResponse>> getActiveEncounters(@RequestParam UUID organizationId) {
        log.info("Getting active encounters for organization: {}", organizationId);
        List<EncounterResponse> responses = encounterService.getActiveEncountersByOrganization(organizationId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active/inpatient")
    @Operation(summary = "Get active inpatient encounters", description = "IPD list: INPATIENT / HOSPITAL_ADMISSION with active status. "
            + "Optional attendingPhysicianId: hospital.doctors.doctor_id, or the portal user id when that user is linked to a doctor (linked_user_id).")
    public ResponseEntity<List<EncounterResponse>> getActiveInpatientEncounters(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID attendingPhysicianId) {
        log.info("Getting active inpatient encounters for organization: {}", organizationId);
        List<EncounterResponse> responses = encounterService.getActiveInpatientEncountersByOrganization(
                organizationId, attendingPhysicianId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{encounterId}")
    @Operation(summary = "Update encounter", description = "Update an existing encounter with automatic length of stay recalculation")
    public ResponseEntity<EncounterResponse> updateEncounter(
            @PathVariable UUID encounterId,
            @Valid @RequestBody EncounterRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating encounter: {}", encounterId);
        EncounterResponse response = encounterService.updateEncounter(encounterId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{encounterId}/status")
    @Operation(summary = "Update encounter status", description = "Update encounter status with automatic date setting based on status")
    public ResponseEntity<EncounterResponse> updateEncounterStatus(
            @PathVariable UUID encounterId,
            @RequestParam String status,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating encounter {} status to: {}", encounterId, status);
        Encounter.EncounterStatus statusEnum = Encounter.EncounterStatus.valueOf(status);
        EncounterResponse response = encounterService.updateEncounterStatus(encounterId, statusEnum, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{encounterId}")
    @Operation(summary = "Delete encounter")
    public ResponseEntity<Void> deleteEncounter(@PathVariable UUID encounterId) {
        log.info("Deleting encounter: {}", encounterId);
        encounterService.deleteEncounter(encounterId);
        return ResponseEntity.noContent().build();
    }
}
