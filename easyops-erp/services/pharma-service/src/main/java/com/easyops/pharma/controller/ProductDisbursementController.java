package com.easyops.pharma.controller;

import com.easyops.pharma.entity.ProductDisbursement;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.ProductDisbursementService;
import com.easyops.pharma.service.TerritoryService;
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
@RequestMapping("/api/pharma/product-disbursements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Disbursement", description = "Product disbursement from depot to territory management APIs")
@CrossOrigin(origins = "*")
public class ProductDisbursementController {

    private final ProductDisbursementService disbursementService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all product disbursements")
    public ResponseEntity<List<ProductDisbursement>> getAllDisbursements(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/product-disbursements - organizationId: {}", organizationId);
        List<ProductDisbursement> disbursements = disbursementService.getAllDisbursements(organizationId);
        return ResponseEntity.ok(disbursements);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get disbursements by territory")
    public ResponseEntity<List<ProductDisbursement>> getDisbursementsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/product-disbursements/territory/{}", territoryId);
        List<ProductDisbursement> disbursements = disbursementService.getDisbursementsByTerritory(territoryId);
        return ResponseEntity.ok(disbursements);
    }

    @GetMapping("/territory/{territoryId}/period")
    @Operation(summary = "Get disbursements by territory and period")
    public ResponseEntity<List<ProductDisbursement>> getDisbursementsByTerritoryAndPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/product-disbursements/territory/{}/period - year: {}, month: {}", territoryId, year, month);
        List<ProductDisbursement> disbursements = disbursementService.getDisbursementsByTerritoryAndPeriod(territoryId, year, month);
        return ResponseEntity.ok(disbursements);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get disbursement by ID")
    public ResponseEntity<ProductDisbursement> getDisbursementById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductDisbursement disbursement = disbursementService.getDisbursementById(id);
        pharmaRbac.requirePharmaView(actor, disbursement.getOrganizationId());
        log.info("GET /api/pharma/product-disbursements/{}", id);
        return ResponseEntity.ok(disbursement);
    }

    @PostMapping
    @Operation(summary = "Create new product disbursement")
    public ResponseEntity<ProductDisbursement> createDisbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ProductDisbursement disbursement) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, disbursement.getOrganizationId());
        log.info("POST /api/pharma/product-disbursements");
        ProductDisbursement created = disbursementService.createDisbursement(disbursement);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product disbursement")
    public ResponseEntity<ProductDisbursement> updateDisbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProductDisbursement disbursement) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, disbursement.getOrganizationId());
        log.info("PUT /api/pharma/product-disbursements/{}", id);
        ProductDisbursement updated = disbursementService.updateDisbursement(id, disbursement);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit product disbursement")
    public ResponseEntity<ProductDisbursement> submitDisbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductDisbursement existing = disbursementService.getDisbursementById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/product-disbursements/{}/submit", id);
        ProductDisbursement disbursement = disbursementService.submitDisbursement(id);
        return ResponseEntity.ok(disbursement);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product disbursement")
    public ResponseEntity<Void> deleteDisbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductDisbursement existing = disbursementService.getDisbursementById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/product-disbursements/{}", id);
        disbursementService.deleteDisbursement(id);
        return ResponseEntity.noContent().build();
    }
}
