package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.PatientProblemRequest;
import com.easyops.hospital.dto.request.ProblemResolutionRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.PatientProblem;
import com.easyops.hospital.entity.ProblemMedication;
import com.easyops.hospital.service.CodeLookupService;
import com.easyops.hospital.service.ProblemListService;
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

@RestController
@RequestMapping("/api/patients/{patientId}/problems")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem List Management", description = "APIs for managing patient problem lists, diagnoses, and problem history")
public class ProblemListController {
    
    private final ProblemListService problemListService;
    private final CodeLookupService codeLookupService;
    
    // ========== Problem CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new problem", description = "Add a new problem/diagnosis to the patient's problem list")
    public ResponseEntity<PatientProblemResponse> createProblem(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientProblemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating problem for patient: {}", patientId);
        if (userId == null) userId = UUID.randomUUID();
        
        // Ensure patient ID matches
        request.setPatientId(patientId);
        
        PatientProblemResponse response = problemListService.createProblem(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all problems for a patient", description = "Retrieve all problems in the patient's problem list, ordered by documented date")
    public ResponseEntity<List<PatientProblemResponse>> getProblems(@PathVariable UUID patientId) {
        List<PatientProblemResponse> responses = problemListService.getProblemsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Get problem list summary", description = "Get comprehensive summary including active, resolved, and high-priority problems")
    public ResponseEntity<ProblemListSummaryResponse> getProblemListSummary(@PathVariable UUID patientId) {
        ProblemListSummaryResponse response = problemListService.getProblemListSummary(patientId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active problems", description = "Retrieve all active problems for a patient, ordered by priority and date")
    public ResponseEntity<List<PatientProblemResponse>> getActiveProblems(@PathVariable UUID patientId) {
        List<PatientProblemResponse> responses = problemListService.getActiveProblemsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/resolved")
    @Operation(summary = "Get resolved problems", description = "Retrieve all resolved problems for a patient, ordered by resolution date")
    public ResponseEntity<List<PatientProblemResponse>> getResolvedProblems(@PathVariable UUID patientId) {
        List<PatientProblemResponse> responses = problemListService.getResolvedProblemsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/current")
    @Operation(summary = "Get current problems", description = "Retrieve current problems (active + chronic) for a patient")
    public ResponseEntity<List<PatientProblemResponse>> getCurrentProblems(@PathVariable UUID patientId) {
        List<PatientProblemResponse> responses = problemListService.getCurrentProblemsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/type/{problemType}")
    @Operation(summary = "Get problems by type", description = "Retrieve problems filtered by problem type (DIAGNOSIS, SYMPTOM, FINDING, etc.)")
    public ResponseEntity<List<PatientProblemResponse>> getProblemsByType(
            @PathVariable UUID patientId,
            @PathVariable PatientProblem.ProblemType problemType) {
        List<PatientProblemResponse> responses = problemListService.getProblemsByPatientAndType(patientId, problemType);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get problems by status", description = "Retrieve problems filtered by status (ACTIVE, RESOLVED, INACTIVE, etc.)")
    public ResponseEntity<List<PatientProblemResponse>> getProblemsByStatus(
            @PathVariable UUID patientId,
            @PathVariable PatientProblem.ProblemStatus status) {
        List<PatientProblemResponse> responses = problemListService.getProblemsByPatientAndStatus(patientId, status);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/priority/{priority}")
    @Operation(summary = "Get problems by priority", description = "Retrieve problems filtered by priority (HIGH, MEDIUM, LOW)")
    public ResponseEntity<List<PatientProblemResponse>> getProblemsByPriority(
            @PathVariable UUID patientId,
            @PathVariable PatientProblem.Priority priority) {
        List<PatientProblemResponse> responses = problemListService.getProblemsByPatientAndPriority(patientId, priority);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search problems", description = "Search problems by name, ICD-10, ICD-11, or SNOMED code")
    public ResponseEntity<List<PatientProblemResponse>> searchProblems(
            @PathVariable UUID patientId,
            @RequestParam String searchTerm) {
        List<PatientProblemResponse> responses = problemListService.searchProblems(patientId, searchTerm);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{problemId}")
    @Operation(summary = "Get problem by ID", description = "Retrieve a specific problem by its ID")
    public ResponseEntity<PatientProblemResponse> getProblemById(@PathVariable UUID problemId) {
        PatientProblemResponse response = problemListService.getProblemById(problemId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{problemId}")
    @Operation(summary = "Update problem", description = "Update an existing problem in the patient's problem list")
    public ResponseEntity<PatientProblemResponse> updateProblem(
            @PathVariable UUID patientId,
            @PathVariable UUID problemId,
            @Valid @RequestBody PatientProblemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating problem: {}", problemId);
        if (userId == null) userId = UUID.randomUUID();
        
        PatientProblemResponse response = problemListService.updateProblem(patientId, problemId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{problemId}")
    @Operation(summary = "Delete problem", description = "Delete a problem from the patient's problem list")
    public ResponseEntity<Void> deleteProblem(@PathVariable UUID patientId, @PathVariable UUID problemId) {
        log.info("Deleting problem: {}", problemId);
        problemListService.deleteProblem(patientId, problemId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Problem Resolution ==========
    
    @PostMapping("/{problemId}/resolve")
    @Operation(summary = "Resolve a problem", description = "Mark a problem as resolved with resolution date and notes")
    public ResponseEntity<PatientProblemResponse> resolveProblem(
            @PathVariable UUID patientId,
            @PathVariable UUID problemId,
            @Valid @RequestBody ProblemResolutionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Resolving problem: {}", problemId);
        if (userId == null) userId = UUID.randomUUID();
        
        PatientProblemResponse response = problemListService.resolveProblem(problemId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{problemId}/reactivate")
    @Operation(summary = "Reactivate a resolved problem", description = "Reactivate a previously resolved problem (changes status back to ACTIVE)")
    public ResponseEntity<PatientProblemResponse> reactivateProblem(
            @PathVariable UUID patientId,
            @PathVariable UUID problemId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Reactivating problem: {}", problemId);
        if (userId == null) userId = UUID.randomUUID();
        
        PatientProblemResponse response = problemListService.reactivateProblem(problemId, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Problem History ==========
    
    @GetMapping("/{problemId}/history")
    @Operation(summary = "Get problem history", description = "Retrieve complete audit trail/history for a problem")
    public ResponseEntity<List<ProblemHistoryResponse>> getProblemHistory(@PathVariable UUID problemId) {
        List<ProblemHistoryResponse> responses = problemListService.getProblemHistory(problemId);
        return ResponseEntity.ok(responses);
    }
    
    // ========== Medication Integration ==========
    
    @PostMapping("/{problemId}/medications/{medicationId}")
    @Operation(summary = "Link medication to problem", description = "Link a medication to a problem/diagnosis")
    public ResponseEntity<ProblemMedicationResponse> linkMedication(
            @PathVariable UUID patientId,
            @PathVariable UUID problemId,
            @PathVariable UUID medicationId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String linkStrength,
            @RequestParam(required = false) String clinicalRelevance,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking medication {} to problem {}", medicationId, problemId);
        if (userId == null) userId = UUID.randomUUID();
        
        ProblemMedication.LinkType type = linkType != null 
            ? ProblemMedication.LinkType.valueOf(linkType) 
            : null;
        ProblemMedication.LinkStrength strength = linkStrength != null 
            ? ProblemMedication.LinkStrength.valueOf(linkStrength) 
            : null;
        
        ProblemMedicationResponse response = problemListService.linkMedicationToProblem(
            problemId, medicationId, type, strength, clinicalRelevance, notes, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{problemId}/medications")
    @Operation(summary = "Get medications linked to problem", description = "Retrieve all medications linked to a problem")
    public ResponseEntity<List<ProblemMedicationResponse>> getMedicationsByProblem(@PathVariable UUID problemId) {
        List<ProblemMedicationResponse> responses = problemListService.getMedicationsByProblem(problemId);
        return ResponseEntity.ok(responses);
    }
    
    @DeleteMapping("/{problemId}/medications/{medicationId}")
    @Operation(summary = "Unlink medication from problem", description = "Remove the link between a medication and a problem")
    public ResponseEntity<Void> unlinkMedication(
            @PathVariable UUID patientId,
            @PathVariable UUID problemId,
            @PathVariable UUID medicationId) {
        log.info("Unlinking medication {} from problem {}", medicationId, problemId);
        problemListService.unlinkMedicationFromProblem(problemId, medicationId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Code Lookup/Suggestions ==========
    
    @GetMapping("/codes/icd10")
    @Operation(summary = "Search ICD-10 codes", description = "Get ICD-10 code suggestions based on search term")
    public ResponseEntity<List<CodeSuggestionResponse>> searchIcd10Codes(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "10") int limit) {
        List<CodeSuggestionResponse> suggestions = codeLookupService.searchIcd10(searchTerm, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/codes/icd11")
    @Operation(summary = "Search ICD-11 codes", description = "Get ICD-11 code suggestions based on search term")
    public ResponseEntity<List<CodeSuggestionResponse>> searchIcd11Codes(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "10") int limit) {
        List<CodeSuggestionResponse> suggestions = codeLookupService.searchIcd11(searchTerm, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/codes/snomed")
    @Operation(summary = "Search SNOMED CT codes", description = "Get SNOMED CT code suggestions based on search term")
    public ResponseEntity<List<CodeSuggestionResponse>> searchSnomedCodes(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "10") int limit) {
        List<CodeSuggestionResponse> suggestions = codeLookupService.searchSnomed(searchTerm, limit);
        return ResponseEntity.ok(suggestions);
    }
}
