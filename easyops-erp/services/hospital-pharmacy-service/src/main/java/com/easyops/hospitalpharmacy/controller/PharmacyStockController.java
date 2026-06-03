package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.StockAdjustmentApprovalDecisionRequest;
import com.easyops.hospitalpharmacy.dto.request.StockAdjustmentRequest;
import com.easyops.hospitalpharmacy.dto.request.StockReceiptRequest;
import com.easyops.hospitalpharmacy.dto.request.StockTransferRequest;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentApprovalResponse;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentMovementResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyStockItemResponse;
import com.easyops.hospitalpharmacy.dto.response.StockTransferMovementResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.PharmacyStockService;
import com.easyops.hospitalpharmacy.service.StockAdjustmentApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/pharmacies/{pharmacyId}/stock")
@RequiredArgsConstructor
@Slf4j
public class PharmacyStockController {

    private final PharmacyStockService pharmacyStockService;
    private final StockAdjustmentApprovalService adjustmentApprovalService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @GetMapping
    public ResponseEntity<List<PharmacyStockItemResponse>> getStock(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyStockService.getStockForLocation(pharmacyId));
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<StockTransferMovementResponse>> getTransfers(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyStockService.getTransfersForLocation(pharmacyId));
    }

    @GetMapping("/adjustments")
    public ResponseEntity<List<StockAdjustmentMovementResponse>> getAdjustments(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyStockService.getAdjustmentsForLocation(pharmacyId));
    }

    @PostMapping("/receipts")
    public ResponseEntity<Void> receiveStock(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @Valid @RequestBody StockReceiptRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        log.info("Receiving stock at pharmacy {} with {} lines", pharmacyId, request.getLines().size());
        pharmacyStockService.receiveStock(pharmacyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> adjustStock(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        UUID approver = request.getApprovedByUserId();
        if (approver == null) {
            request.setApprovedByUserId(actor);
        } else if (!approver.equals(actor)) {
            hospitalPharmacyRbac.requireHospitalManage(approver, organizationId);
        }
        log.info("Adjusting stock at pharmacy {} with {} lines", pharmacyId, request.getLines().size());
        pharmacyStockService.adjustStock(pharmacyId, request, actor);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/adjustments/pending-approvals")
    public ResponseEntity<List<StockAdjustmentApprovalResponse>> listPendingApprovals(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireStockAdjustmentApprove(actor, organizationId);
        return ResponseEntity.ok(adjustmentApprovalService.listPendingByLocation(pharmacyId));
    }

    @PostMapping("/adjustments/approvals/{approvalId}/approve")
    public ResponseEntity<StockAdjustmentApprovalResponse> approveAdjustment(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @PathVariable UUID approvalId,
            @Valid @RequestBody StockAdjustmentApprovalDecisionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireStockAdjustmentApprove(actor, organizationId);
        request.setDecision("APPROVED");
        return ResponseEntity.ok(adjustmentApprovalService.decide(approvalId, request, actor));
    }

    @PostMapping("/adjustments/approvals/{approvalId}/reject")
    public ResponseEntity<StockAdjustmentApprovalResponse> rejectAdjustment(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @PathVariable UUID approvalId,
            @Valid @RequestBody StockAdjustmentApprovalDecisionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireStockAdjustmentApprove(actor, organizationId);
        request.setDecision("REJECTED");
        return ResponseEntity.ok(adjustmentApprovalService.decide(approvalId, request, actor));
    }

    @PostMapping("/transfers")
    public ResponseEntity<Void> transferStock(
            @PathVariable("pharmacyId") UUID pharmacyId,
            @Valid @RequestBody StockTransferRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        UUID approver = request.getApprovedByUserId();
        if (approver == null) {
            request.setApprovedByUserId(actor);
        } else if (!approver.equals(actor)) {
            hospitalPharmacyRbac.requireHospitalManage(approver, organizationId);
        }
        log.info("Transferring stock from pharmacy {} to {} with {} lines",
                pharmacyId, request.getDestinationPharmacyLocationId(), request.getLines().size());
        pharmacyStockService.transferStock(pharmacyId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
