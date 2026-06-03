package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.LabResultRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.service.LabResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Laboratory Result Management", description = "APIs for laboratory result receipt, storage, and management")
public class LabResultController {
    
    private final LabResultService labResultService;
    
    // ========== Lab Result CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new lab result", description = "Receive and store a new laboratory test result")
    public ResponseEntity<LabResultResponse> createLabResult(
            @Valid @RequestBody LabResultRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating lab result for order: {}", request.getOrderId());
        if (userId == null) userId = UUID.randomUUID();
        LabResultResponse response = labResultService.createLabResult(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{resultId}")
    @Operation(summary = "Get lab result by ID", description = "Retrieve a lab result by its ID")
    public ResponseEntity<LabResultResponse> getLabResultById(@PathVariable UUID resultId) {
        LabResultResponse response = labResultService.getLabResultById(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{resultNumber}")
    @Operation(summary = "Get lab result by number", description = "Retrieve a lab result by its result number")
    public ResponseEntity<LabResultResponse> getLabResultByNumber(@PathVariable String resultNumber) {
        LabResultResponse response = labResultService.getLabResultByNumber(resultNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all lab results for a patient", description = "Retrieve all lab results for a patient, ordered by date")
    public ResponseEntity<List<LabResultResponse>> getLabResultsByPatient(@PathVariable UUID patientId) {
        List<LabResultResponse> responses = labResultService.getLabResultsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get lab results by order", description = "Retrieve all lab results for a specific order")
    public ResponseEntity<List<LabResultResponse>> getLabResultsByOrder(@PathVariable UUID orderId) {
        List<LabResultResponse> responses = labResultService.getLabResultsByOrder(orderId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/encounters/{encounterId}")
    @Operation(summary = "Get lab results by encounter", description = "Retrieve all lab results for a specific encounter/visit")
    public ResponseEntity<List<LabResultResponse>> getLabResultsByEncounter(@PathVariable UUID encounterId) {
        List<LabResultResponse> responses = labResultService.getLabResultsByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/critical/unacknowledged")
    @Operation(summary = "Get unacknowledged critical values", description = "Retrieve unacknowledged critical values for a patient")
    public ResponseEntity<List<LabResultResponse>> getUnacknowledgedCriticalValues(@PathVariable UUID patientId) {
        List<LabResultResponse> responses = labResultService.getUnacknowledgedCriticalValues(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/critical/unacknowledged")
    @Operation(summary = "Get all unacknowledged critical values", description = "Retrieve all unacknowledged critical values")
    public ResponseEntity<List<LabResultResponse>> getAllUnacknowledgedCriticalValues() {
        List<LabResultResponse> responses = labResultService.getAllUnacknowledgedCriticalValues();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/unreviewed")
    @Operation(summary = "Get unreviewed results", description = "Retrieve unreviewed lab results for a patient")
    public ResponseEntity<List<LabResultResponse>> getUnreviewedResults(@PathVariable UUID patientId) {
        List<LabResultResponse> responses = labResultService.getUnreviewedResults(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/abnormal")
    @Operation(summary = "Get abnormal results", description = "Retrieve abnormal lab results for a patient")
    public ResponseEntity<List<LabResultResponse>> getAbnormalResults(@PathVariable UUID patientId) {
        List<LabResultResponse> responses = labResultService.getAbnormalResults(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{resultId}")
    @Operation(summary = "Update lab result", description = "Update a lab result (only allowed for PRELIMINARY status)")
    public ResponseEntity<LabResultResponse> updateLabResult(
            @PathVariable UUID resultId,
            @Valid @RequestBody LabResultRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating lab result: {}", resultId);
        if (userId == null) userId = UUID.randomUUID();
        LabResultResponse response = labResultService.updateLabResult(resultId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{resultId}/review")
    @Operation(summary = "Review lab result", description = "Mark a lab result as reviewed")
    public ResponseEntity<LabResultResponse> reviewLabResult(
            @PathVariable UUID resultId,
            @RequestParam(required = false) String reviewNotes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Reviewing lab result: {}", resultId);
        if (userId == null) userId = UUID.randomUUID();
        if (reviewNotes == null) reviewNotes = "";
        LabResultResponse response = labResultService.reviewLabResult(resultId, reviewNotes, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{resultId}/acknowledge-critical")
    @Operation(summary = "Acknowledge critical value", description = "Acknowledge a critical value alert")
    public ResponseEntity<LabResultResponse> acknowledgeCriticalValue(
            @PathVariable UUID resultId,
            @RequestParam(required = false) String response,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Acknowledging critical value for result: {}", resultId);
        if (userId == null) userId = UUID.randomUUID();
        if (response == null) response = "Acknowledged";
        LabResultResponse labResultResponse = labResultService.acknowledgeCriticalValue(resultId, response, userId);
        return ResponseEntity.ok(labResultResponse);
    }
    
    // ========== Result Display and Viewing Endpoints ==========
    
    @GetMapping("/patients/{patientId}/chronological")
    @Operation(summary = "Get lab results in chronological order", 
               description = "Retrieve all lab results for a patient in chronological order with highlighting")
    public ResponseEntity<List<LabResultListViewResponse>> getLabResultsChronological(
            @PathVariable UUID patientId) {
        List<LabResultListViewResponse> responses = labResultService.getLabResultsChronological(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/by-category")
    @Operation(summary = "Get lab results grouped by category", 
               description = "Retrieve all lab results for a patient grouped by test category")
    public ResponseEntity<Map<String, List<LabResultListViewResponse>>> getLabResultsByCategory(
            @PathVariable UUID patientId) {
        Map<String, List<LabResultListViewResponse>> responses = labResultService.getLabResultsByCategory(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/category/{category}")
    @Operation(summary = "Get lab results by specific category", 
               description = "Retrieve lab results for a patient filtered by test category")
    public ResponseEntity<List<LabResultListViewResponse>> getLabResultsByCategory(
            @PathVariable UUID patientId,
            @PathVariable String category) {
        List<LabResultListViewResponse> responses = labResultService.getLabResultsByCategory(patientId, category);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{resultId}/detail")
    @Operation(summary = "Get detailed lab result view", 
               description = "Retrieve a comprehensive detailed view of a lab result")
    public ResponseEntity<LabResultResponse> getLabResultDetail(@PathVariable UUID resultId) {
        LabResultResponse response = labResultService.getLabResultDetail(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{resultId}/compare")
    @Operation(summary = "Compare result with previous", 
               description = "Compare current lab result with the previous result of the same test")
    public ResponseEntity<LabResultComparisonResponse> compareResults(@PathVariable UUID resultId) {
        LabResultComparisonResponse response = labResultService.compareResults(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}/trend")
    @Operation(summary = "Get trend data for a test", 
               description = "Retrieve trend data (graphs/charts) for a specific test over time")
    public ResponseEntity<LabResultTrendResponse> getTrendData(
            @PathVariable UUID patientId,
            @RequestParam String loincCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        LabResultTrendResponse response = labResultService.getTrendData(patientId, loincCode, startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{resultId}/correlated")
    @Operation(summary = "Get correlated results", 
               description = "Retrieve related test results that should be displayed together")
    public ResponseEntity<LabResultCorrelationResponse> getCorrelatedResults(@PathVariable UUID resultId) {
        LabResultCorrelationResponse response = labResultService.getCorrelatedResults(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}/correlated")
    @Operation(summary = "Get correlated results by collection date", 
               description = "Retrieve all results collected on the same date/time for correlation")
    public ResponseEntity<LabResultCorrelationResponse> getCorrelatedResultsByDate(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime collectionDate) {
        LabResultCorrelationResponse response = labResultService.getCorrelatedResultsByDate(patientId, collectionDate);
        return ResponseEntity.ok(response);
    }
    
    // ========== Result Interpretation and Clinical Context Endpoints ==========
    
    @PostMapping("/{resultId}/link-problem")
    @Operation(summary = "Link lab result to problem/diagnosis", 
               description = "Create a link between a lab result and a patient problem/diagnosis")
    public ResponseEntity<com.easyops.hospital.entity.LabResultProblem> linkResultToProblem(
            @PathVariable UUID resultId,
            @RequestParam UUID problemId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String linkStrength,
            @RequestParam(required = false) String clinicalRelevance,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        com.easyops.hospital.entity.LabResultProblem.LinkType type = linkType != null ? 
            com.easyops.hospital.entity.LabResultProblem.LinkType.valueOf(linkType) : 
            com.easyops.hospital.entity.LabResultProblem.LinkType.RELATED;
        com.easyops.hospital.entity.LabResultProblem.LinkStrength strength = linkStrength != null ? 
            com.easyops.hospital.entity.LabResultProblem.LinkStrength.valueOf(linkStrength) : 
            com.easyops.hospital.entity.LabResultProblem.LinkStrength.MODERATE;
        com.easyops.hospital.entity.LabResultProblem response = labResultService
            .linkResultToProblem(resultId, problemId, type, strength, clinicalRelevance, notes, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{resultId}/link-medication")
    @Operation(summary = "Link lab result to medication", 
               description = "Create a link between a lab result and a medication/prescription")
    public ResponseEntity<com.easyops.hospital.entity.LabResultMedication> linkResultToMedication(
            @PathVariable UUID resultId,
            @RequestParam UUID prescriptionId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String linkStrength,
            @RequestParam(required = false) String clinicalRelevance,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        com.easyops.hospital.entity.LabResultMedication.LinkType type = linkType != null ? 
            com.easyops.hospital.entity.LabResultMedication.LinkType.valueOf(linkType) : 
            com.easyops.hospital.entity.LabResultMedication.LinkType.MONITORS;
        com.easyops.hospital.entity.LabResultMedication.LinkStrength strength = linkStrength != null ? 
            com.easyops.hospital.entity.LabResultMedication.LinkStrength.valueOf(linkStrength) : 
            com.easyops.hospital.entity.LabResultMedication.LinkStrength.MODERATE;
        com.easyops.hospital.entity.LabResultMedication response = labResultService
            .linkResultToMedication(resultId, prescriptionId, type, strength, clinicalRelevance, notes, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{resultId}/linked-problems")
    @Operation(summary = "Get problems linked to lab result", 
               description = "Retrieve all problems/diagnoses linked to a lab result")
    public ResponseEntity<List<com.easyops.hospital.entity.LabResultProblem>> getLinkedProblems(
            @PathVariable UUID resultId) {
        List<com.easyops.hospital.entity.LabResultProblem> response = labResultService.getLinkedProblems(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{resultId}/linked-medications")
    @Operation(summary = "Get medications linked to lab result", 
               description = "Retrieve all medications/prescriptions linked to a lab result")
    public ResponseEntity<List<com.easyops.hospital.entity.LabResultMedication>> getLinkedMedications(
            @PathVariable UUID resultId) {
        List<com.easyops.hospital.entity.LabResultMedication> response = labResultService.getLinkedMedications(resultId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{resultId}/unlink-problem/{problemId}")
    @Operation(summary = "Unlink lab result from problem", 
               description = "Remove the link between a lab result and a problem/diagnosis")
    public ResponseEntity<Void> unlinkResultFromProblem(
            @PathVariable UUID resultId,
            @PathVariable UUID problemId) {
        labResultService.unlinkResultFromProblem(resultId, problemId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{resultId}/unlink-medication/{prescriptionId}")
    @Operation(summary = "Unlink lab result from medication", 
               description = "Remove the link between a lab result and a medication/prescription")
    public ResponseEntity<Void> unlinkResultFromMedication(
            @PathVariable UUID resultId,
            @PathVariable UUID prescriptionId) {
        labResultService.unlinkResultFromMedication(resultId, prescriptionId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{resultId}/link-clinical-note")
    @Operation(summary = "Link lab result to clinical note", 
               description = "Create a link between a lab result and a clinical note")
    public ResponseEntity<com.easyops.hospital.entity.LabResultClinicalNote> linkResultToClinicalNote(
            @PathVariable UUID resultId,
            @RequestParam UUID noteId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String linkStrength,
            @RequestParam(required = false) String clinicalRelevance,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        com.easyops.hospital.entity.LabResultClinicalNote.LinkType type = linkType != null ? 
            com.easyops.hospital.entity.LabResultClinicalNote.LinkType.valueOf(linkType) : 
            com.easyops.hospital.entity.LabResultClinicalNote.LinkType.REFERENCED;
        com.easyops.hospital.entity.LabResultClinicalNote.LinkStrength strength = linkStrength != null ? 
            com.easyops.hospital.entity.LabResultClinicalNote.LinkStrength.valueOf(linkStrength) : 
            com.easyops.hospital.entity.LabResultClinicalNote.LinkStrength.MODERATE;
        com.easyops.hospital.entity.LabResultClinicalNote response = labResultService
            .linkResultToClinicalNote(resultId, noteId, type, strength, clinicalRelevance, notes, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{resultId}/linked-clinical-notes")
    @Operation(summary = "Get clinical notes linked to lab result", 
               description = "Retrieve all clinical notes linked to a lab result")
    public ResponseEntity<List<com.easyops.hospital.entity.LabResultClinicalNote>> getLinkedClinicalNotes(
            @PathVariable UUID resultId) {
        List<com.easyops.hospital.entity.LabResultClinicalNote> response = labResultService.getLinkedClinicalNotes(resultId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{resultId}/unlink-clinical-note/{noteId}")
    @Operation(summary = "Unlink lab result from clinical note", 
               description = "Remove the link between a lab result and a clinical note")
    public ResponseEntity<Void> unlinkResultFromClinicalNote(
            @PathVariable UUID resultId,
            @PathVariable UUID noteId) {
        labResultService.unlinkResultFromClinicalNote(resultId, noteId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{resultId}/drug-lab-interactions")
    @Operation(summary = "Get drug-lab interaction alerts for result", 
               description = "Retrieve all drug-lab interaction alerts for a lab result")
    public ResponseEntity<List<com.easyops.hospital.entity.DrugLabInteractionAlert>> getDrugLabInteractionAlerts(
            @PathVariable UUID resultId) {
        List<com.easyops.hospital.entity.DrugLabInteractionAlert> response = 
            labResultService.getDrugLabInteractionAlerts(resultId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}/drug-lab-interactions")
    @Operation(summary = "Get drug-lab interaction alerts for patient", 
               description = "Retrieve all active drug-lab interaction alerts for a patient")
    public ResponseEntity<List<com.easyops.hospital.entity.DrugLabInteractionAlert>> getDrugLabInteractionAlertsByPatient(
            @PathVariable UUID patientId) {
        List<com.easyops.hospital.entity.DrugLabInteractionAlert> response = 
            labResultService.getDrugLabInteractionAlertsByPatient(patientId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/drug-lab-interactions/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge drug-lab interaction alert", 
               description = "Acknowledge a drug-lab interaction alert")
    public ResponseEntity<com.easyops.hospital.entity.DrugLabInteractionAlert> acknowledgeDrugLabAlert(
            @PathVariable UUID alertId,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        if (notes == null) notes = "";
        com.easyops.hospital.entity.DrugLabInteractionAlert response = 
            labResultService.acknowledgeDrugLabAlert(alertId, notes, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Test Panel Result Values ==========
    
    @GetMapping("/{resultId}/panel-values")
    @Operation(summary = "Get panel result values", 
               description = "Retrieve all individual test result values for a test panel result")
    public ResponseEntity<List<com.easyops.hospital.dto.response.LabResultValueResponse>> getPanelResultValues(
            @PathVariable UUID resultId) {
        List<com.easyops.hospital.dto.response.LabResultValueResponse> responses = 
            labResultService.getPanelResultValues(resultId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/orders/{orderId}/panel-values")
    @Operation(summary = "Get panel order values", 
               description = "Retrieve all individual test result values for a test panel order")
    public ResponseEntity<List<com.easyops.hospital.dto.response.LabResultValueResponse>> getPanelOrderValues(
            @PathVariable UUID orderId) {
        List<com.easyops.hospital.dto.response.LabResultValueResponse> responses = 
            labResultService.getPanelOrderValues(orderId);
        return ResponseEntity.ok(responses);
    }
}
