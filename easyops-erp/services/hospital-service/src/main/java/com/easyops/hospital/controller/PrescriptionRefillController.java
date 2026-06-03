package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.service.PrescriptionRefillService;
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
@RequestMapping("/api/prescription-refills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prescription Refill Management", description = "APIs for prescription refill requests, approval workflow, and refill tracking")
public class PrescriptionRefillController {
    
    private final PrescriptionRefillService prescriptionRefillService;
    
    // ========== Refill Request Management ==========
    
    @PostMapping("/requests")
    @Operation(summary = "Create refill request", description = "Create a new prescription refill request from pharmacy, patient, or provider")
    public ResponseEntity<PrescriptionRefillRequestResponse> createRefillRequest(
            @Valid @RequestBody RefillRequestRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating refill request for prescription: {}", request.getPrescriptionId());
        if (userId == null) userId = UUID.randomUUID();
        PrescriptionRefillRequestResponse response = prescriptionRefillService.createRefillRequest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/requests")
    @Operation(summary = "Get pending refill requests", description = "Get all pending refill requests ordered by urgency (refill request queue)")
    public ResponseEntity<List<PrescriptionRefillRequestResponse>> getPendingRefillRequests() {
        List<PrescriptionRefillRequestResponse> responses = prescriptionRefillService.getPendingRefillRequests();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/requests/{refillRequestId}")
    @Operation(summary = "Get refill request by ID", description = "Retrieve a specific refill request by its ID")
    public ResponseEntity<PrescriptionRefillRequestResponse> getRefillRequestById(
            @PathVariable UUID refillRequestId) {
        PrescriptionRefillRequestResponse response = prescriptionRefillService.getRefillRequestById(refillRequestId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/prescriptions/{prescriptionId}/requests")
    @Operation(summary = "Get refill requests for prescription", description = "Retrieve all refill requests for a specific prescription")
    public ResponseEntity<List<PrescriptionRefillRequestResponse>> getRefillRequestsByPrescription(
            @PathVariable UUID prescriptionId) {
        List<PrescriptionRefillRequestResponse> responses = 
            prescriptionRefillService.getRefillRequestsByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/requests")
    @Operation(summary = "Get refill requests for patient", description = "Retrieve all refill requests for a specific patient")
    public ResponseEntity<List<PrescriptionRefillRequestResponse>> getRefillRequestsByPatient(
            @PathVariable UUID patientId) {
        List<PrescriptionRefillRequestResponse> responses = 
            prescriptionRefillService.getRefillRequestsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    // ========== Refill Approval Workflow ==========
    
    @PostMapping("/requests/{refillRequestId}/approve")
    @Operation(summary = "Approve refill request", description = "Approve a pending refill request")
    public ResponseEntity<PrescriptionRefillRequestResponse> approveRefillRequest(
            @PathVariable UUID refillRequestId,
            @RequestBody(required = false) RefillApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Approving refill request: {}", refillRequestId);
        if (userId == null) userId = UUID.randomUUID();
        if (request == null) {
            request = RefillApprovalRequest.builder().build();
        }
        PrescriptionRefillRequestResponse response = 
            prescriptionRefillService.approveRefillRequest(refillRequestId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/requests/{refillRequestId}/deny")
    @Operation(summary = "Deny refill request", description = "Deny a pending refill request with reason")
    public ResponseEntity<PrescriptionRefillRequestResponse> denyRefillRequest(
            @PathVariable UUID refillRequestId,
            @Valid @RequestBody RefillDenialRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Denying refill request: {}", refillRequestId);
        if (userId == null) userId = UUID.randomUUID();
        PrescriptionRefillRequestResponse response = 
            prescriptionRefillService.denyRefillRequest(refillRequestId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/requests/{refillRequestId}/modify")
    @Operation(summary = "Modify refill request", description = "Modify a pending refill request (approve with different quantity)")
    public ResponseEntity<PrescriptionRefillRequestResponse> modifyRefillRequest(
            @PathVariable UUID refillRequestId,
            @Valid @RequestBody RefillModificationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Modifying refill request: {}", refillRequestId);
        if (userId == null) userId = UUID.randomUUID();
        PrescriptionRefillRequestResponse response = 
            prescriptionRefillService.modifyRefillRequest(refillRequestId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Prescription Refill Recording ==========
    
    @PostMapping("/refills")
    @Operation(summary = "Record prescription refill", description = "Record a prescription refill when pharmacy fills it")
    public ResponseEntity<PrescriptionRefillResponse> recordRefill(
            @Valid @RequestBody PrescriptionRefillRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Recording refill for prescription: {}", request.getPrescriptionId());
        if (userId == null) userId = UUID.randomUUID();
        PrescriptionRefillResponse response = prescriptionRefillService.recordRefill(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/prescriptions/{prescriptionId}/refills")
    @Operation(summary = "Get refills for prescription", description = "Retrieve all refills for a specific prescription")
    public ResponseEntity<List<PrescriptionRefillResponse>> getRefillsByPrescription(
            @PathVariable UUID prescriptionId) {
        List<PrescriptionRefillResponse> responses = 
            prescriptionRefillService.getRefillsByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/refills")
    @Operation(summary = "Get refills for patient", description = "Retrieve all refills for a specific patient")
    public ResponseEntity<List<PrescriptionRefillResponse>> getRefillsByPatient(
            @PathVariable UUID patientId) {
        List<PrescriptionRefillResponse> responses = prescriptionRefillService.getRefillsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
}
