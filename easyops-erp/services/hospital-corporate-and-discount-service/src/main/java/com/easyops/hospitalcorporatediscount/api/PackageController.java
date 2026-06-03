package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.domain.packagedefinition.PackageService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/packages")
    public ResponseEntity<PackageResponse> create(
            @Valid @RequestBody CreatePackageRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        PackageResponse created = packageService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/packages/{id}")
    public PackageDetailResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return packageService.getById(id);
    }

    @GetMapping("/packages")
    public PagedResponse<PackageResponse> list(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return packageService.list(code, isPublic, page, size);
    }

    @PatchMapping("/packages/{id}")
    public PackageResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdatePackageRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return packageService.update(id, request);
    }

    @PostMapping("/packages/{id}/items")
    public ResponseEntity<PackageItemResponse> addItem(
            @PathVariable("id") UUID packageId,
            @Valid @RequestBody CreatePackageItemRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        PackageItemResponse created = packageService.addItem(packageId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/packages/{id}/items")
    public List<PackageItemResponse> listItems(
            @PathVariable("id") UUID packageId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return packageService.listItems(packageId);
    }

    @DeleteMapping("/packages/{packageId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("packageId") UUID packageId,
            @PathVariable("itemId") UUID itemId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        packageService.deleteItem(packageId, itemId);
        return ResponseEntity.noContent().build();
    }
}
