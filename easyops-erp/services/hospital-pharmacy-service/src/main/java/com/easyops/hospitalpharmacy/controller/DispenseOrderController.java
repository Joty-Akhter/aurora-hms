package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.CreateDispenseOrderRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseReturnRequest;
import com.easyops.hospitalpharmacy.dto.request.DispenseUnfulfilledLineRequest;
import com.easyops.hospitalpharmacy.dto.request.PatchDispenseOrderRegionalRequest;
import com.easyops.hospitalpharmacy.dto.response.BillableDispenseItemResponse;
import com.easyops.hospitalpharmacy.dto.response.DispenseOrderResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.DispenseIdempotencyService;
import com.easyops.hospitalpharmacy.service.DispenseOrderService;
import com.easyops.hospitalpharmacy.service.DispenseReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/dispense-orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Hospital pharmacy — dispense orders")
public class DispenseOrderController {

    private final DispenseOrderService dispenseOrderService;
    private final DispenseIdempotencyService dispenseIdempotencyService;
    private final DispenseReceiptService dispenseReceiptService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    @Operation(summary = "Create dispense order")
    public ResponseEntity<DispenseOrderResponse> createDispenseOrder(
            @Valid @RequestBody CreateDispenseOrderRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        log.info("Creating dispense order (prescriptionId={}) at pharmacy {}", request.getPrescriptionId(), request.getPharmacyLocationId());
        DispenseOrderResponse response = dispenseOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispense order by id")
    public ResponseEntity<DispenseOrderResponse> getDispenseOrder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(dispenseOrderService.getById(id));
    }

    /**
     * Billable line items for charge preview / posting (Phase P1 — read-only; pricing from billing when integrated).
     */
    @GetMapping("/{id}/billable-items")
    @Operation(summary = "List billable line items (billing preview; prices when billing is integrated)")
    public ResponseEntity<List<BillableDispenseItemResponse>> getBillableItems(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(dispenseOrderService.getBillableItems(id));
    }

    @GetMapping
    @Operation(summary = "Search dispense orders")
    public ResponseEntity<List<DispenseOrderResponse>> searchDispenseOrders(
            @RequestParam(value = "patientId", required = false) String patientId,
            @RequestParam(value = "visitId", required = false) String visitId,
            @RequestParam(value = "pharmacyLocationId", required = false) UUID pharmacyLocationId,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(dispenseOrderService.search(patientId, visitId, pharmacyLocationId, status));
    }

    @PostMapping("/{id}/lines")
    @Operation(summary = "Add dispense lines (deduct stock)")
    public ResponseEntity<DispenseOrderResponse> addDispenseLines(
            @PathVariable UUID id,
            @Valid @RequestBody @NotEmpty(message = "At least one dispense line is required") List<@Valid DispenseLineRequest> requests,
            @Parameter(description = "Optional; same key replays stored response without duplicating stock moves")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        log.info("Recording {} dispense lines for order {}", requests.size(), id);
        boolean anyOverride = requests.stream()
                .anyMatch(r -> r.getStockOverrideReason() != null && !r.getStockOverrideReason().isBlank());
        if (anyOverride) {
            hospitalPharmacyRbac.requireStockOverrideOrHospitalManage(actor, organizationId);
        }
        boolean anyFormularyOverride = requests.stream()
                .anyMatch(r -> r.getFormularyOverrideReason() != null && !r.getFormularyOverrideReason().isBlank());
        if (anyFormularyOverride) {
            hospitalPharmacyRbac.requireStockOverrideOrHospitalManage(actor, organizationId);
        }
        boolean anyClinicalSafetyOverride = requests.stream()
                .anyMatch(r -> r.getClinicalSafetyOverrideReason() != null && !r.getClinicalSafetyOverrideReason().isBlank());
        if (anyClinicalSafetyOverride) {
            hospitalPharmacyRbac.requireStockOverrideOrHospitalManage(actor, organizationId);
        }
        DispenseOrderResponse response = dispenseIdempotencyService.executePostDispenseLines(
                id, idempotencyKey, () -> dispenseOrderService.addDispenseLines(id, requests, actor, organizationId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/lines/unfulfilled")
    @Operation(summary = "Record a line as out of stock or refused without issuing stock (WS-C)")
    public ResponseEntity<DispenseOrderResponse> recordUnfulfilledLine(
            @PathVariable UUID id,
            @Valid @RequestBody DispenseUnfulfilledLineRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(dispenseOrderService.recordUnfulfilledLine(id, request));
    }

    @PostMapping("/{id}/returns")
    @Operation(summary = "Record returns against dispense lines")
    public ResponseEntity<DispenseOrderResponse> recordReturns(
            @PathVariable UUID id,
            @Valid @RequestBody DispenseReturnRequest request,
            @Parameter(description = "Optional; same key replays stored response without duplicating return postings")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        log.info("Recording {} return lines for order {}", request.getLines().size(), id);
        DispenseOrderResponse response = dispenseIdempotencyService.executePostReturns(
                id, idempotencyKey, () -> dispenseOrderService.recordReturns(id, request, actor, organizationId));
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download dispense receipt PDF (reprint audited)")
    public ResponseEntity<byte[]> getReceiptPdf(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        byte[] pdf = dispenseReceiptService.buildReceiptPdf(id);
        dispenseReceiptService.recordReceiptPrinted(id, actor);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"dispense-receipt-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PatchMapping("/{id}/regional")
    @Operation(summary = "Update paper Rx / external validation fields (Phase P3)")
    public ResponseEntity<DispenseOrderResponse> patchRegional(
            @PathVariable UUID id,
            @Valid @RequestBody PatchDispenseOrderRegionalRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(dispenseOrderService.patchRegional(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update dispense order status")
    public ResponseEntity<DispenseOrderResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam("status") String status,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        log.info("Updating status for dispense order {} to {}", id, status);
        DispenseOrderResponse response = dispenseOrderService.updateStatus(id, status, actor, organizationId);
        return ResponseEntity.ok(response);
    }
}
