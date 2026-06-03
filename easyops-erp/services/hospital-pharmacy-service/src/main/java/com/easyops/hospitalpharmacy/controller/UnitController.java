package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.UnitRequest;
import com.easyops.hospitalpharmacy.dto.response.UnitResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/units")
@RequiredArgsConstructor
@Slf4j
public class UnitController {

    private final UnitService unitService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<UnitResponse> create(
            @Valid @RequestBody UnitRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Creating unit: {}", request.getName());
        UnitResponse response = unitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UnitResponse>> list(
            @RequestParam(value = "baseOnly", required = false, defaultValue = "false") boolean baseOnly,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        List<UnitResponse> units = baseOnly ? unitService.listBaseUnits() : unitService.listAll();
        return ResponseEntity.ok(units);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(unitService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UnitResponse> update(
            @PathVariable UUID id,
            @RequestBody UnitRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Updating unit: {}", id);
        return ResponseEntity.ok(unitService.update(id, request));
    }
}
