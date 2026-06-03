package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.SupplierReturnOrderRequest;
import com.easyops.hospitalpharmacy.dto.response.SupplierReturnOrderResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.SupplierReturnOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/supplier-return-orders")
@RequiredArgsConstructor
@Slf4j
public class SupplierReturnOrderController {

    private final SupplierReturnOrderService returnOrderService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<SupplierReturnOrderResponse> create(
            @Valid @RequestBody SupplierReturnOrderRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnOrderService.create(request, actor));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SupplierReturnOrderResponse> submit(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(returnOrderService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SupplierReturnOrderResponse> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        return ResponseEntity.ok(returnOrderService.approve(id, actor));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<SupplierReturnOrderResponse> dispatch(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(returnOrderService.dispatch(id, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierReturnOrderResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(returnOrderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SupplierReturnOrderResponse>> list(
            @RequestParam(required = false) UUID fromLocationId,
            @RequestParam(required = false) UUID manufacturerId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        if (fromLocationId != null) {
            return ResponseEntity.ok(returnOrderService.listByFromLocation(fromLocationId));
        } else if (manufacturerId != null) {
            return ResponseEntity.ok(returnOrderService.listByManufacturer(manufacturerId));
        }
        return ResponseEntity.ok(returnOrderService.listAll());
    }
}
