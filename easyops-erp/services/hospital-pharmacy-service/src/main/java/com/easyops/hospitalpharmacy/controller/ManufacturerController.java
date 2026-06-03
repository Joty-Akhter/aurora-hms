package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.ManufacturerRequest;
import com.easyops.hospitalpharmacy.dto.response.ManufacturerResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/manufacturers")
@RequiredArgsConstructor
@Slf4j
public class ManufacturerController {

    private final ManufacturerService manufacturerService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<ManufacturerResponse> create(
            @Valid @RequestBody ManufacturerRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Creating manufacturer: {}", request.getName());
        ManufacturerResponse response = manufacturerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManufacturerResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(manufacturerService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ManufacturerResponse>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(manufacturerService.search(name, activeOnly));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ManufacturerResponse> update(
            @PathVariable UUID id,
            @RequestBody ManufacturerRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Updating manufacturer: {}", id);
        return ResponseEntity.ok(manufacturerService.update(id, request));
    }
}
