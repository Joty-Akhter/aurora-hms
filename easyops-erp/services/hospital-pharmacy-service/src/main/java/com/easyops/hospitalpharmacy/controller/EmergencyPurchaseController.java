package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.EmergencyPurchaseEntryRequest;
import com.easyops.hospitalpharmacy.dto.response.EmergencyPurchaseEntryResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.EmergencyPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/emergency-purchases")
@RequiredArgsConstructor
@Slf4j
public class EmergencyPurchaseController {

    private final EmergencyPurchaseService emergencyPurchaseService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<EmergencyPurchaseEntryResponse> create(
            @Valid @RequestBody EmergencyPurchaseEntryRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(emergencyPurchaseService.create(request, actor));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<EmergencyPurchaseEntryResponse> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireEmergencyPurchaseApprove(actor, organizationId);
        return ResponseEntity.ok(emergencyPurchaseService.approve(id, actor));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<EmergencyPurchaseEntryResponse> receive(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyDispenseMutate(actor, organizationId);
        return ResponseEntity.ok(emergencyPurchaseService.receive(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyPurchaseEntryResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(emergencyPurchaseService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<EmergencyPurchaseEntryResponse>> list(
            @RequestParam(required = false) UUID toLocationId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        if (toLocationId != null) {
            return ResponseEntity.ok(emergencyPurchaseService.listByLocation(toLocationId));
        }
        return ResponseEntity.ok(emergencyPurchaseService.listAll());
    }
}
