package com.easyops.pharma.controller;

import com.easyops.pharma.entity.Adjustment;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.AdjustmentService;
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
@RequestMapping("/api/pharma/adjustments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Adjustment Management", description = "Damage and expiry adjustment management APIs")
@CrossOrigin(origins = "*")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all adjustments")
    public ResponseEntity<List<Adjustment>> getAllAdjustments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/adjustments - organizationId: {}", organizationId);
        List<Adjustment> adjustments = adjustmentService.getAllAdjustments(organizationId);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get adjustments by territory")
    public ResponseEntity<List<Adjustment>> getAdjustmentsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/adjustments/territory/{}", territoryId);
        List<Adjustment> adjustments = adjustmentService.getAdjustmentsByTerritory(territoryId);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/territory/{territoryId}/period")
    @Operation(summary = "Get adjustments by territory and period")
    public ResponseEntity<List<Adjustment>> getAdjustmentsByTerritoryAndPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/adjustments/territory/{}/period - year: {}, month: {}", territoryId, year, month);
        List<Adjustment> adjustments = adjustmentService.getAdjustmentsByTerritoryAndPeriod(territoryId, year, month);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get adjustment by ID")
    public ResponseEntity<Adjustment> getAdjustmentById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Adjustment adjustment = adjustmentService.getAdjustmentById(id);
        pharmaRbac.requirePharmaView(actor, adjustment.getOrganizationId());
        log.info("GET /api/pharma/adjustments/{}", id);
        return ResponseEntity.ok(adjustment);
    }

    @PostMapping
    @Operation(summary = "Create new adjustment")
    public ResponseEntity<Adjustment> createAdjustment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Adjustment adjustment) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, adjustment.getOrganizationId());
        log.info("POST /api/pharma/adjustments");
        Adjustment created = adjustmentService.createAdjustment(adjustment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update adjustment")
    public ResponseEntity<Adjustment> updateAdjustment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Adjustment adjustment) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, adjustment.getOrganizationId());
        log.info("PUT /api/pharma/adjustments/{}", id);
        Adjustment updated = adjustmentService.updateAdjustment(id, adjustment);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit adjustment")
    public ResponseEntity<Adjustment> submitAdjustment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Adjustment existing = adjustmentService.getAdjustmentById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/adjustments/{}/submit", id);
        Adjustment adjustment = adjustmentService.submitAdjustment(id);
        return ResponseEntity.ok(adjustment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete adjustment")
    public ResponseEntity<Void> deleteAdjustment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Adjustment existing = adjustmentService.getAdjustmentById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/adjustments/{}", id);
        adjustmentService.deleteAdjustment(id);
        return ResponseEntity.noContent().build();
    }
}
