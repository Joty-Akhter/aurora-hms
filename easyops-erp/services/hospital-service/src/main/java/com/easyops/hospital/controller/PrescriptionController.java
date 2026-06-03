package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.service.FormularyService;
import com.easyops.hospital.service.PriorAuthorizationService;
import com.easyops.hospital.service.PrescriptionService;
import com.easyops.hospital.service.PDMPService;
import com.easyops.hospital.service.EPrescribingService;
import com.easyops.hospital.service.PrescriptionRefillService;
import com.easyops.hospital.service.RbacPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prescription Management", description = "APIs for electronic prescription creation, drug interaction checking, allergy checking, and e-prescribing")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionRefillService prescriptionRefillService;
    private final FormularyService formularyService;
    private final PriorAuthorizationService priorAuthorizationService;
    private final PDMPService pdmpService;
    private final EPrescribingService eprescribingService;
    private final RbacPermissionService rbacPermissionService;

    // ========== Prescription CRUD Operations ==========

    @PostMapping
    @Operation(summary = "Create a new prescription", description = "Create a new electronic prescription with automatic validation")
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @Valid @RequestBody PrescriptionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Creating prescription for patient: {}", request.getPatientId());
        PrescriptionResponse response = prescriptionService.createPrescription(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{prescriptionId}")
    @Operation(summary = "Get prescription by ID", description = "Retrieve a prescription by its ID")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        PrescriptionResponse response = prescriptionService.getPrescriptionById(prescriptionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{prescriptionNumber}")
    @Operation(summary = "Get prescription by number", description = "Retrieve a prescription by its prescription number")
    public ResponseEntity<PrescriptionResponse> getPrescriptionByNumber(
            @PathVariable String prescriptionNumber,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        PrescriptionResponse response = prescriptionService.getPrescriptionByNumber(prescriptionNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all prescriptions for a patient", description = "Retrieve all prescriptions for a patient, ordered by creation date")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        List<PrescriptionResponse> responses = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/patients/{patientId}/active")
    @Operation(summary = "Get active prescriptions", description = "Retrieve active prescriptions (SENT, FILLED, PARTIALLY_FILLED) for a patient")
    public ResponseEntity<List<PrescriptionResponse>> getActivePrescriptions(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        List<PrescriptionResponse> responses = prescriptionService.getActivePrescriptionsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/patients/{patientId}/drafts")
    @Operation(summary = "Get draft prescriptions", description = "Retrieve draft prescriptions for a patient")
    public ResponseEntity<List<PrescriptionResponse>> getDraftPrescriptions(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        List<PrescriptionResponse> responses = prescriptionService.getDraftPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{prescriptionId}")
    @Operation(summary = "Update prescription", description = "Update a prescription (only allowed for DRAFT status)")
    public ResponseEntity<PrescriptionResponse> updatePrescription(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody PrescriptionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Updating prescription: {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.updatePrescription(prescriptionId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{prescriptionId}")
    @Operation(summary = "Delete prescription", description = "Delete a prescription (only allowed for DRAFT status)")
    public ResponseEntity<Void> deletePrescription(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Deleting prescription: {}", prescriptionId);
        prescriptionService.deletePrescription(prescriptionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Req-J2 / FR-P1.10: canonical REST path for refill requests; {@code prescriptionId} in the path
     * always wins over any duplicate field in the body. Schedule II (DEA C-II) is rejected with HTTP 422
     * in the service layer before a refill-request row is inserted.
     */
    @PostMapping("/{prescriptionId}/refill-requests")
    @Operation(summary = "Create refill request (prescription-scoped path)",
               description = "Same behaviour as POST /api/prescription-refills/requests but binds the prescription "
                           + "in the URL. Body prescriptionId, if present, is overwritten by the path parameter.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refill request created"),
            @ApiResponse(responseCode = "422", description = "Schedule II — refills prohibited (Req-J2); or other business rule")
    })
    public ResponseEntity<PrescriptionRefillRequestResponse> createRefillRequestForPrescription(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody RefillRequestRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating refill request for prescription (scoped path): {}", prescriptionId);
        if (userId == null) {
            userId = UUID.randomUUID();
        }
        request.setPrescriptionId(prescriptionId);
        PrescriptionRefillRequestResponse response = prescriptionRefillService.createRefillRequest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== Drug Interaction Checking ==========

    @PostMapping("/check-interactions")
    @Operation(summary = "Check drug interactions", description = "Check for drug interactions between a medication and existing prescriptions")
    public ResponseEntity<DrugInteractionCheckResponse> checkDrugInteractions(
            @Valid @RequestBody DrugInteractionCheckRequest request,
            @RequestParam UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking drug interactions for patient: {}", patientId);
        DrugInteractionCheckResponse response = prescriptionService.checkDrugInteractions(request, patientId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{prescriptionId}/check-interactions")
    @Operation(summary = "Check prescription interactions", description = "Check for drug interactions for a specific prescription")
    public ResponseEntity<PrescriptionResponse> checkPrescriptionInteractions(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking interactions for prescription: {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.checkPrescriptionInteractions(prescriptionId);
        return ResponseEntity.ok(response);
    }

    // ========== Allergy Checking ==========

    @PostMapping("/check-allergies")
    @Operation(summary = "Check allergies", description = "Check for patient allergies to a medication")
    public ResponseEntity<AllergyCheckResponse> checkAllergies(
            @Valid @RequestBody AllergyCheckRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking allergies for patient: {}", request.getPatientId());
        AllergyCheckResponse response = prescriptionService.checkAllergies(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{prescriptionId}/check-allergies")
    @Operation(summary = "Check prescription allergies", description = "Check for patient allergies for a specific prescription")
    public ResponseEntity<PrescriptionResponse> checkPrescriptionAllergies(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking allergies for prescription: {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.checkPrescriptionAllergies(prescriptionId);
        return ResponseEntity.ok(response);
    }

    // ========== Prescription Validation ==========

    @PostMapping("/{prescriptionId}/validate")
    @Operation(summary = "Validate prescription", description = "Validate a prescription for errors and warnings")
    public ResponseEntity<PrescriptionResponse> validatePrescription(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Validating prescription: {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.validatePrescription(prescriptionId);
        return ResponseEntity.ok(response);
    }

    // ========== Prescription Transmission ==========

    @PostMapping("/{prescriptionId}/transmit")
    @Operation(summary = "Transmit prescription", description = "Transmit prescription to pharmacy (e-prescribing)")
    public ResponseEntity<PrescriptionResponse> transmitPrescription(
            @PathVariable UUID prescriptionId,
            @RequestBody(required = false) PrescriptionTransmitRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionTransmit(userId, organizationId);
        log.info("Transmitting prescription: {}", prescriptionId);
        if (request == null) {
            request = PrescriptionTransmitRequest.builder()
                .overrideInteractions(false)
                .overrideAllergies(false)
                .overridePdmpCheck(false)
                .build();
        }
        PrescriptionResponse response = prescriptionService.transmitPrescription(prescriptionId, request, userId);
        return ResponseEntity.ok(response);
    }

    // ========== Interaction & Allergy Acknowledgment ==========

    @GetMapping("/{prescriptionId}/interactions")
    @Operation(summary = "Get prescription interactions",
               description = "Get all recorded drug interactions for a prescription")
    public ResponseEntity<List<PrescriptionInteractionResponse>> getPrescriptionInteractions(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting interactions for prescription: {}", prescriptionId);
        List<PrescriptionInteractionResponse> responses =
                prescriptionService.getInteractionsByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{prescriptionId}/interactions/{interactionId}/acknowledge")
    @Operation(summary = "Acknowledge drug interaction",
               description = "Acknowledge a drug interaction with a clinical override reason; persists isAcknowledged=true on the interaction record for audit compliance")
    public ResponseEntity<PrescriptionInteractionResponse> acknowledgeInteraction(
            @PathVariable UUID prescriptionId,
            @PathVariable UUID interactionId,
            @RequestParam(required = false) String overrideReason,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Acknowledging interaction {} for prescription {}", interactionId, prescriptionId);
        if (overrideReason == null || overrideReason.isBlank()) {
            overrideReason = "Acknowledged by provider";
        }
        PrescriptionInteractionResponse response =
                prescriptionService.acknowledgeInteraction(prescriptionId, interactionId, overrideReason, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/allergy-checks")
    @Operation(summary = "Get prescription allergy checks",
               description = "Get all recorded allergy checks for a prescription")
    public ResponseEntity<List<PrescriptionAllergyCheckResponse>> getPrescriptionAllergyChecks(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting allergy checks for prescription: {}", prescriptionId);
        List<PrescriptionAllergyCheckResponse> responses =
                prescriptionService.getAllergyChecksByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{prescriptionId}/allergy-checks/{checkId}/acknowledge")
    @Operation(summary = "Acknowledge allergy check",
               description = "Acknowledge an allergy warning with a clinical override reason; marks actionTaken=OVERRIDDEN and persists isAcknowledged=true for compliance audit")
    public ResponseEntity<PrescriptionAllergyCheckResponse> acknowledgeAllergyCheck(
            @PathVariable UUID prescriptionId,
            @PathVariable UUID checkId,
            @RequestParam(required = false) String overrideReason,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Acknowledging allergy check {} for prescription {}", checkId, prescriptionId);
        if (overrideReason == null || overrideReason.isBlank()) {
            overrideReason = "Acknowledged by provider";
        }
        PrescriptionAllergyCheckResponse response =
                prescriptionService.acknowledgeAllergyCheck(prescriptionId, checkId, overrideReason, userId);
        return ResponseEntity.ok(response);
    }

    // ========== Prescription Status Management ==========

    @PostMapping("/{prescriptionId}/cancel")
    @Operation(summary = "Cancel prescription", description = "Cancel a prescription with reason")
    public ResponseEntity<PrescriptionResponse> cancelPrescription(
            @PathVariable UUID prescriptionId,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Cancelling prescription: {}", prescriptionId);
        if (reason == null) reason = "Cancelled by provider";
        PrescriptionResponse response = prescriptionService.cancelPrescription(prescriptionId, reason, userId);
        return ResponseEntity.ok(response);
    }

    // ========== PDMP (Prescription Drug Monitoring Program) Integration ==========

    @PostMapping("/{prescriptionId}/pdmp/query")
    @Operation(summary = "Query PDMP", description = "Query Prescription Drug Monitoring Program for patient's controlled substance history")
    public ResponseEntity<PDMPQueryResponse> queryPDMP(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody PDMPQueryRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestHeader(value = "X-Provider-Npi", required = false) String providerNpi,
            @RequestHeader(value = "X-Provider-Name", required = false) String providerName) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Querying PDMP for prescription: {}", prescriptionId);
        request.setPrescriptionId(prescriptionId);
        PDMPQueryResponse response = pdmpService.queryPDMP(request, userId, providerNpi, providerName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/pdmp/results")
    @Operation(summary = "Get PDMP query results", description = "Get all PDMP query results for a prescription")
    public ResponseEntity<List<PDMPQueryResponse>> getPDMPQueryResults(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting PDMP query results for prescription: {}", prescriptionId);
        List<PDMPQueryResponse> responses = pdmpService.getPDMPQueryResults(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{prescriptionId}/pdmp/results/latest")
    @Operation(summary = "Get latest PDMP query result", description = "Get the most recent PDMP query result for a prescription")
    public ResponseEntity<PDMPQueryResponse> getLatestPDMPQueryResult(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting latest PDMP query result for prescription: {}", prescriptionId);
        PDMPQueryResponse response = pdmpService.getLatestPDMPQueryResult(prescriptionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/pdmp/results")
    @Operation(summary = "Get PDMP query results for patient", description = "Get all PDMP query results for a patient")
    public ResponseEntity<List<PDMPQueryResponse>> getPDMPQueryResultsByPatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting PDMP query results for patient: {}", patientId);
        List<PDMPQueryResponse> responses = pdmpService.getPDMPQueryResultsByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    // ========== Formulary Integration ==========

    @PostMapping("/{prescriptionId}/formulary/check")
    @Operation(summary = "Check formulary coverage", description = "Check insurance formulary coverage for a prescription")
    public ResponseEntity<FormularyCheckResponse> checkFormularyCoverage(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody FormularyCheckRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking formulary coverage for prescription: {}", prescriptionId);
        request.setPrescriptionId(prescriptionId);
        FormularyCheckResponse response = formularyService.checkFormularyCoverage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/formulary/check/latest")
    @Operation(summary = "Get latest formulary check", description = "Get the most recent formulary check result for a prescription")
    public ResponseEntity<FormularyCheckResponse> getLatestFormularyCheck(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting latest formulary check for prescription: {}", prescriptionId);
        FormularyCheckResponse response = formularyService.getLatestFormularyCheck(prescriptionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/formulary/check/history")
    @Operation(summary = "Get formulary check history", description = "Get all formulary check results for a prescription")
    public ResponseEntity<List<FormularyCheckResponse>> getFormularyCheckHistory(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting formulary check history for prescription: {}", prescriptionId);
        List<FormularyCheckResponse> responses = formularyService.getFormularyCheckHistory(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{prescriptionId}/formulary/alternatives")
    @Operation(summary = "Get formulary alternatives", description = "Get formulary alternatives for a prescription")
    public ResponseEntity<List<FormularyCheckResponse.FormularyAlternativeResponse>> getFormularyAlternatives(
            @PathVariable UUID prescriptionId,
            @RequestParam UUID formularyCheckId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting formulary alternatives for prescription: {}", prescriptionId);
        List<FormularyCheckResponse.FormularyAlternativeResponse> responses =
                formularyService.getFormularyAlternatives(formularyCheckId);
        return ResponseEntity.ok(responses);
    }

    // ========== Prior Authorization ==========

    @PostMapping("/{prescriptionId}/prior-authorization/submit")
    @Operation(summary = "Submit prior authorization request", description = "Submit a prior authorization request to insurance/PBM")
    public ResponseEntity<PriorAuthorizationResponse> submitPriorAuthorization(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody PriorAuthorizationRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Submitting prior authorization for prescription: {}", prescriptionId);
        request.setPrescriptionId(prescriptionId);
        PriorAuthorizationResponse response = priorAuthorizationService.submitPriorAuthorization(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/prior-authorization")
    @Operation(summary = "Get prior authorizations", description = "Get all prior authorization requests for a prescription")
    public ResponseEntity<List<PriorAuthorizationResponse>> getPriorAuthorizations(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting prior authorizations for prescription: {}", prescriptionId);
        List<PriorAuthorizationResponse> responses = priorAuthorizationService.getPriorAuthorizationsByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/prior-authorization/{priorAuthId}")
    @Operation(summary = "Get prior authorization by ID", description = "Get a specific prior authorization request")
    public ResponseEntity<PriorAuthorizationResponse> getPriorAuthorization(
            @PathVariable UUID priorAuthId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting prior authorization: {}", priorAuthId);
        PriorAuthorizationResponse response = priorAuthorizationService.getPriorAuthorization(priorAuthId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/prior-authorization/{priorAuthId}/check-status")
    @Operation(summary = "Check prior authorization status", description = "Check the status of a prior authorization request with PBM")
    public ResponseEntity<PriorAuthorizationResponse> checkPriorAuthorizationStatus(
            @PathVariable UUID priorAuthId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Checking prior authorization status: {}", priorAuthId);
        PriorAuthorizationResponse response = priorAuthorizationService.checkPriorAuthorizationStatus(priorAuthId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/prior-authorization/{priorAuthId}")
    @Operation(summary = "Update prior authorization status",
               description = "Manually update the status of a prior authorization (e.g. mark as APPROVED or DENIED after receiving payer decision via phone/fax)")
    public ResponseEntity<PriorAuthorizationResponse> updatePriorAuthorizationStatus(
            @PathVariable UUID priorAuthId,
            @RequestParam com.easyops.hospital.entity.PriorAuthorization.PriorAuthStatus status,
            @RequestParam(required = false) String priorAuthNumber,
            @RequestParam(required = false) String expirationDate,
            @RequestParam(required = false) String denialReason,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionPrescribe(userId, organizationId);
        log.info("Updating prior authorization {} to status {}", priorAuthId, status);
        java.time.LocalDate expDate = null;
        if (expirationDate != null && !expirationDate.isBlank()) {
            expDate = java.time.LocalDate.parse(expirationDate);
        }
        PriorAuthorizationResponse response = priorAuthorizationService.updatePriorAuthorizationStatus(
                priorAuthId, status, priorAuthNumber, expDate, denialReason, userId);
        return ResponseEntity.ok(response);
    }

    // ========== E-Prescribing Network Integration ==========

    @PostMapping("/{prescriptionId}/transmit/network")
    @Operation(summary = "Transmit prescription via e-prescribing network",
               description = "Transmit prescription to pharmacy via e-prescribing network (Surescripts, etc.)")
    public ResponseEntity<PrescriptionTransmissionResponse> transmitPrescriptionViaNetwork(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody(required = false) PrescriptionTransmissionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId,
            @RequestHeader(value = "X-Provider-Npi", required = false) String providerNpi,
            @RequestHeader(value = "X-Provider-Name", required = false) String providerName) {
        rbacPermissionService.requirePrescriptionTransmit(userId, organizationId);
        log.info("Transmitting prescription via e-prescribing network: {}", prescriptionId);
        if (request == null) {
            request = PrescriptionTransmissionRequest.builder()
                .prescriptionId(prescriptionId)
                .build();
        } else {
            request.setPrescriptionId(prescriptionId);
        }

        PrescriptionTransmissionResponse response = eprescribingService.transmitPrescription(
            request, userId, providerName, providerNpi);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}/transmissions")
    @Operation(summary = "Get prescription transmissions",
               description = "Get all transmission records for a prescription")
    public ResponseEntity<List<PrescriptionTransmissionResponse>> getPrescriptionTransmissions(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting transmissions for prescription: {}", prescriptionId);
        List<PrescriptionTransmissionResponse> responses = eprescribingService.getTransmissionsByPrescription(prescriptionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{prescriptionId}/transmissions/latest")
    @Operation(summary = "Get latest transmission",
               description = "Get the most recent transmission record for a prescription")
    public ResponseEntity<PrescriptionTransmissionResponse> getLatestTransmission(
            @PathVariable UUID prescriptionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting latest transmission for prescription: {}", prescriptionId);
        PrescriptionTransmissionResponse response = eprescribingService.getLatestTransmission(prescriptionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transmissions/{transmissionId}")
    @Operation(summary = "Get transmission by ID",
               description = "Get a specific transmission record by ID")
    public ResponseEntity<PrescriptionTransmissionResponse> getTransmission(
            @PathVariable UUID transmissionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionView(userId, organizationId);
        log.info("Getting transmission: {}", transmissionId);
        PrescriptionTransmissionResponse response = eprescribingService.getTransmission(transmissionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transmissions/{transmissionId}/retry")
    @Operation(summary = "Retry failed transmission",
               description = "Retry a failed prescription transmission")
    public ResponseEntity<PrescriptionTransmissionResponse> retryTransmission(
            @PathVariable UUID transmissionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacPermissionService.requirePrescriptionTransmit(userId, organizationId);
        log.info("Retrying transmission: {}", transmissionId);
        PrescriptionTransmissionResponse response = eprescribingService.retryTransmission(transmissionId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transmissions/fill-status")
    @Operation(
        summary = "Pharmacy fill-status callback (FR-P3.11a)",
        description = """
            Inbound webhook called by the pharmacy or e-prescribing network to report a fill-status \
            update for a previously transmitted prescription.

            **fillStatus accepted values (all 10):**
            `PENDING`, `IN_PROGRESS`, `ON_HOLD`, `OUT_OF_STOCK`, `PARTIALLY_FILLED`, `FILLED`, \
            `PICKED_UP`, `CANCELLED`, `REJECTED`, `EXPIRED`.

            **Terminal states:** `PICKED_UP`, `CANCELLED`, `REJECTED`, `EXPIRED` — \
            any subsequent transition away from a terminal state is rejected with **HTTP 409**.

            **Idempotency:** duplicate callbacks with the same `networkTransactionId` + `fillStatus` \
            are acknowledged with HTTP 200 and produce no side-effects.

            **fillStatusDate enforcement:** `fillStatusDate` is always required. \
            A null or absent value is rejected with **HTTP 400**. \
            The server never substitutes `now()` — the timestamp must originate from the pharmacy \
            system to preserve accurate fill-event timing for audit and billing reconciliation.

            **Conditional fields:**
            - `filledDate` — required when `fillStatus` is `FILLED` or `PARTIALLY_FILLED`
            - `pickedUpDate` — required when `fillStatus` is `PICKED_UP`
            - `cancellationReason` — required (non-blank) when `fillStatus` is `CANCELLED` or `REJECTED`
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fill status recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Request validation failed — missing required field (e.g. fillStatusDate, filledDate, cancellationReason) or a conditional constraint violated"),
        @ApiResponse(responseCode = "401", description = "HMAC-SHA256 webhook signature missing or invalid"),
        @ApiResponse(responseCode = "404", description = "networkTransactionId not found"),
        @ApiResponse(responseCode = "409", description = "Fill-status state-machine violation: attempted transition from a terminal state, or an otherwise invalid transition per the state machine")
    })
    public ResponseEntity<PrescriptionTransmissionResponse> updateFillStatus(
            @Valid @RequestBody FillStatusUpdateRequest request) {
        log.info("Updating fill status for transaction: {}", request.getNetworkTransactionId());
        PrescriptionTransmissionResponse response = eprescribingService.updateFillStatus(request);
        return ResponseEntity.ok(response);
    }
}
