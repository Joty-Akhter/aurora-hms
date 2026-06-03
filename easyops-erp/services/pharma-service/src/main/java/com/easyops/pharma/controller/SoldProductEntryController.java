package com.easyops.pharma.controller;

import com.easyops.pharma.entity.SoldProductEntry;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.SoldProductEntryService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/sold-product-entries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sold Product Entry", description = "Product-wise sales entry APIs")
@CrossOrigin(origins = "*")
public class SoldProductEntryController {

    private final SoldProductEntryService soldProductEntryService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all sold product entries")
    public ResponseEntity<List<SoldProductEntry>> getAll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/sold-product-entries - organizationId: {}", organizationId);
        List<SoldProductEntry> entries = soldProductEntryService.getAll(organizationId);
        log.info("Returning {} sold product entries", entries.size());
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get entries by territory")
    public ResponseEntity<List<SoldProductEntry>> getByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        return ResponseEntity.ok(soldProductEntryService.getByTerritory(territoryId));
    }

    @GetMapping("/territory/{territoryId}/period")
    @Operation(summary = "Get entries by territory and period")
    public ResponseEntity<List<SoldProductEntry>> getByTerritoryAndPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        return ResponseEntity.ok(soldProductEntryService.getByTerritoryAndPeriod(territoryId, year, month));
    }

    @GetMapping("/territory/{territoryId}/outstanding-quantity")
    @Operation(summary = "Get outstanding quantity for a product in a territory")
    public ResponseEntity<BigDecimal> getOutstandingQuantity(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("productId") UUID productId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        return ResponseEntity.ok(soldProductEntryService.getOutstandingQuantity(territoryId, productId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get entry by ID")
    public ResponseEntity<SoldProductEntry> getById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        SoldProductEntry entry = soldProductEntryService.getById(id);
        pharmaRbac.requirePharmaView(actor, entry.getOrganizationId());
        return ResponseEntity.ok(entry);
    }

    @PostMapping
    @Operation(summary = "Create sold product entry")
    public ResponseEntity<SoldProductEntry> create(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody SoldProductEntry entry) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, entry.getOrganizationId());
        SoldProductEntry created = soldProductEntryService.create(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update sold product entry")
    public ResponseEntity<SoldProductEntry> update(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SoldProductEntry entry) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, entry.getOrganizationId());
        return ResponseEntity.ok(soldProductEntryService.update(id, entry));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit sold product entry")
    public ResponseEntity<SoldProductEntry> submit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        SoldProductEntry existing = soldProductEntryService.getById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(soldProductEntryService.submit(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete sold product entry")
    public ResponseEntity<SoldProductEntry> complete(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        SoldProductEntry existing = soldProductEntryService.getById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(soldProductEntryService.complete(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft sold product entry")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        SoldProductEntry existing = soldProductEntryService.getById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        soldProductEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
