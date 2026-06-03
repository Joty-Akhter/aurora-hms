package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.StockRequisitionApprovalRequest;
import com.easyops.hospitalpharmacy.dto.request.StockRequisitionRequest;
import com.easyops.hospitalpharmacy.dto.response.StockRequisitionResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.StockRequisitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/stock-requisitions")
@RequiredArgsConstructor
@Slf4j
public class StockRequisitionController {

    private final StockRequisitionService requisitionService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<StockRequisitionResponse> create(
            @Valid @RequestBody StockRequisitionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionService.create(request, actor));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<StockRequisitionResponse> submit(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(requisitionService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<StockRequisitionResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody StockRequisitionApprovalRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireRequisitionApprove(actor, organizationId);
        return ResponseEntity.ok(requisitionService.approve(id, request, actor));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<StockRequisitionResponse> reject(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireRequisitionApprove(actor, organizationId);
        return ResponseEntity.ok(requisitionService.reject(id, notes, actor));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<StockRequisitionResponse> receive(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(requisitionService.receive(id, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockRequisitionResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(requisitionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<StockRequisitionResponse>> list(
            @RequestParam(required = false) UUID fromLocationId,
            @RequestParam(required = false) UUID toLocationId,
            @RequestParam(required = false) String status,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        if (fromLocationId != null) {
            return ResponseEntity.ok(requisitionService.listByFromLocation(fromLocationId));
        } else if (toLocationId != null) {
            return ResponseEntity.ok(requisitionService.listByToLocation(toLocationId));
        } else if (status != null) {
            return ResponseEntity.ok(requisitionService.listByStatus(status));
        }
        return ResponseEntity.ok(requisitionService.listAll());
    }
}
