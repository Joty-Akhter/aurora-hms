package com.easyops.ap.controller;

import com.easyops.ap.dto.VendorRequest;
import com.easyops.ap.entity.Vendor;
import com.easyops.ap.security.AccountingRbacService;
import com.easyops.ap.security.RbacRequestHeaders;
import com.easyops.ap.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ap/vendors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AP Vendors", description = "Vendor management for Accounts Payable")
public class VendorController {

    private final VendorService vendorService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all vendors for an organization")
    public ResponseEntity<List<Vendor>> getAllVendors(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        List<Vendor> vendors = activeOnly
                ? vendorService.getActiveVendors(organizationId)
                : vendorService.getAllVendors(organizationId);
        return ResponseEntity.ok(vendors);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID")
    public ResponseEntity<Vendor> getVendorById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Vendor vendor = vendorService.getVendorById(id);
        accountingRbac.requireAccountingView(actor, vendor.getOrganizationId());
        return ResponseEntity.ok(vendor);
    }

    @PostMapping
    @Operation(summary = "Create new vendor")
    public ResponseEntity<Vendor> createVendor(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody VendorRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.createVendor(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vendor")
    public ResponseEntity<Vendor> updateVendor(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody VendorRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Vendor existing = vendorService.getVendorById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(vendorService.updateVendor(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vendor")
    public ResponseEntity<Void> deleteVendor(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Vendor existing = vendorService.getVendorById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        vendorService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }
}
