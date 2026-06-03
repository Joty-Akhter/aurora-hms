package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.PatientRequest;
import com.easyops.hospital.dto.request.PatientIdentityCardReplaceRequest;
import com.easyops.hospital.dto.response.DuplicatePatientResponse;
import com.easyops.hospital.dto.response.PatientIdentityCardActionResponse;
import com.easyops.hospital.dto.response.PatientIdentityCardPrintResponse;
import com.easyops.hospital.dto.response.PatientResponse;
import com.easyops.hospital.service.PatientService;
import com.easyops.hospital.service.RbacPermissionService;
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
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Management", description = "APIs for patient registration and demographics management")
public class PatientController {
    
    private final PatientService patientService;
    private final RbacPermissionService rbacPermissionService;
    
    @GetMapping
    @Operation(summary = "Get all patients", description = "Retrieve a list of all patients")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        List<PatientResponse> responses = patientService.getAllPatients();
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Create a new patient", description = "Register a new patient with demographic information. "
        + "Returns 409 with duplicate details if potential duplicates exist unless X-Acknowledge-Duplicate: true.")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Acknowledge-Duplicate", required = false) String acknowledgeDuplicate) {
        log.info("Creating new patient: {}", request.getFullName());

        boolean ack = acknowledgeDuplicate != null && "true".equalsIgnoreCase(acknowledgeDuplicate.trim());
        PatientResponse response = patientService.createPatient(request, userId, ack);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{patientId}")
    @Operation(
            summary = "Get patient by ID",
            description =
                    "Retrieve patient demographics. "
                            + "When **X-User-Id** is present, identity card fields are resolved from Hospital Card Service; "
                            + "when omitted (e.g. internal service-to-service calls), those fields are left unset.")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable UUID patientId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        PatientResponse response = patientService.getPatientById(patientId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/mrn/{mrn}")
    @Operation(
            summary = "Get patient by MRN",
            description =
                    "Retrieve patient demographics by MRN. "
                            + "Identity card fields are populated only when **X-User-Id** is sent (same as GET by id).")
    public ResponseEntity<PatientResponse> getPatientByMrn(
            @PathVariable String mrn,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        PatientResponse response = patientService.getPatientByMrn(mrn, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search patients", description = "Search patients by name, MRN, or other criteria")
    public ResponseEntity<List<PatientResponse>> searchPatients(
            @RequestParam String searchTerm) {
        List<PatientResponse> responses = patientService.searchPatients(searchTerm);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/check-duplicates")
    @Operation(
            summary = "Check for duplicate patients",
            description = "Check if a patient already exists. Supports partial payloads (e.g. phone-only) for inline booking hints.")
    public ResponseEntity<DuplicatePatientResponse> checkForDuplicates(@RequestBody PatientRequest request) {
        DuplicatePatientResponse response = patientService.checkForDuplicates(request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{patientId}")
    @Operation(summary = "Update patient", description = "Update patient demographic information")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating patient: {}", patientId);

        PatientResponse response = patientService.updatePatient(patientId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{patientId}")
    @Operation(summary = "Delete patient", description = "Soft delete patient by setting status to ARCHIVED")
    public ResponseEntity<Void> deletePatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Deleting patient: {}", patientId);

        patientService.deletePatient(patientId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/generate-mrn")
    @Operation(summary = "Generate MRN", description = "Generate a new Medical Record Number")
    public ResponseEntity<String> generateMrn(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        String mrn = patientService.generateMrn(organizationId);
        return ResponseEntity.ok(mrn);
    }

    @GetMapping("/{patientId}/identity-card/print-preview")
    @Operation(
            summary = "Get identity card print preview",
            description =
                    "Returns printable HTML without writing a print audit row. "
                            + "Requires same permission as printing; prefer POST /identity-card/reprint when staff prints "
                            + "(audits PRINT or REPRINT).")
    public ResponseEntity<PatientIdentityCardPrintResponse> getIdentityCardPrintPreview(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requireHospitalManage(userId, organizationId);
        return ResponseEntity.ok(patientService.getIdentityCardPrintPreview(patientId, userId));
    }

    @PostMapping("/{patientId}/identity-card/reprint")
    @Operation(
            summary = "Print or reprint identity card",
            description = "Returns printable HTML and appends a PRINT (first) or REPRINT audit row to patient_identity_card_audit_log.")
    public ResponseEntity<PatientIdentityCardPrintResponse> reprintIdentityCard(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requireHospitalManage(userId, organizationId);
        return ResponseEntity.ok(patientService.reprintIdentityCard(patientId, userId));
    }

    @PostMapping("/{patientId}/identity-card/replace")
    @Operation(summary = "Replace patient identity card", description = "Replaces current card with new card number and writes replacement audit record")
    public ResponseEntity<PatientIdentityCardActionResponse> replaceIdentityCard(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @Valid @RequestBody PatientIdentityCardReplaceRequest request) {
        rbacPermissionService.requireHospitalManage(userId, organizationId);
        return ResponseEntity.ok(patientService.replaceIdentityCard(patientId, userId, request.getReason()));
    }
}
