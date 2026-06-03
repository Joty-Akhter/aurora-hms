package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.request.ProductGroupRequest;
import com.easyops.hospitalpharmacy.dto.response.ProductGroupResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.ProductGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/product-groups")
@RequiredArgsConstructor
@Slf4j
public class ProductGroupController {

    private final ProductGroupService productGroupService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @PostMapping
    public ResponseEntity<ProductGroupResponse> create(
            @Valid @RequestBody ProductGroupRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Creating product group: {}", request.getName());
        ProductGroupResponse response = productGroupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductGroupResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(productGroupService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductGroupResponse>> list(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalView(actor, organizationId);
        return ResponseEntity.ok(productGroupService.list(activeOnly));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductGroupResponse> update(
            @PathVariable UUID id,
            @RequestBody ProductGroupRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireHospitalManage(actor, organizationId);
        log.info("Updating product group: {}", id);
        return ResponseEntity.ok(productGroupService.update(id, request));
    }
}
