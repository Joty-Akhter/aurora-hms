package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.PharmacyLocationRequest;
import com.easyops.hospitalpharmacy.dto.response.PharmacyLocationResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.PharmacyLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/pharmacies")
@RequiredArgsConstructor
@Slf4j
public class PharmacyLocationController {

    private final PharmacyLocationService pharmacyLocationService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<PharmacyLocationResponse> create(
            @Valid @RequestBody PharmacyLocationRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Creating pharmacy location: {}", request.getName());
        PharmacyLocationResponse response = pharmacyLocationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacyLocationResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(pharmacyLocationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PharmacyLocationResponse>> list(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(pharmacyLocationService.list(activeOnly));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PharmacyLocationResponse> update(
            @PathVariable UUID id,
            @RequestBody PharmacyLocationRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Updating pharmacy location: {}", id);
        return ResponseEntity.ok(pharmacyLocationService.update(id, request));
    }
}
