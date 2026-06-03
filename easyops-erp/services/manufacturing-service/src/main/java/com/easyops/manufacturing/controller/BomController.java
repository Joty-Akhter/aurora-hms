package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.BomHeader;
import com.easyops.manufacturing.entity.BomLine;
import com.easyops.manufacturing.entity.BomVersion;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.BomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/boms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BomController {

    private final BomService bomService;
    private final ManufacturingRbacService manufacturingRbac;

    // ==================== BOM Header Endpoints ====================

    @GetMapping
    public ResponseEntity<List<BomHeader>> getAllBoms(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<BomHeader> boms = bomService.getAllBomsByOrganization(organizationId);
        return ResponseEntity.ok(boms);
    }

    @GetMapping("/{bomId}")
    public ResponseEntity<BomHeader> getBomById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return bomService.getBomById(bomId)
                .map(bom -> {
                    manufacturingRbac.requireManufacturingView(actor, bom.getOrganizationId());
                    return ResponseEntity.ok(bom);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{bomNumber}")
    public ResponseEntity<BomHeader> getBomByNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String bomNumber,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        return bomService.getBomByNumber(organizationId, bomNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<BomHeader>> getBomsByProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<BomHeader> boms = bomService.getBomsByProduct(organizationId, productId);
        return ResponseEntity.ok(boms);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BomHeader>> getActiveBoms(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<BomHeader> boms = bomService.getActiveBoms(organizationId);
        return ResponseEntity.ok(boms);
    }

    @GetMapping("/product/{productId}/latest")
    public ResponseEntity<BomHeader> getLatestBomForProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return bomService.getLatestActiveBomForProduct(productId)
                .map(bom -> {
                    manufacturingRbac.requireManufacturingView(actor, bom.getOrganizationId());
                    return ResponseEntity.ok(bom);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BomHeader> createBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody BomHeader bomHeader) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, bomHeader.getOrganizationId());
        BomHeader created = bomService.createBom(bomHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{bomId}")
    public ResponseEntity<BomHeader> updateBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId,
            @RequestBody BomHeader bomHeader) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomHeader updated = bomService.updateBom(bomId, bomHeader);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{bomId}")
    public ResponseEntity<Void> deleteBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        bomService.deleteBom(bomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bomId}/approve")
    public ResponseEntity<BomHeader> approveBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId,
            @RequestParam UUID approvedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomHeader approved = bomService.approveBom(bomId, approvedBy);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{bomId}/recalculate-costs")
    public ResponseEntity<BomHeader> recalculateCosts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomHeader bom = bomService.recalculateBomCosts(bomId);
        return ResponseEntity.ok(bom);
    }

    // ==================== BOM Line Endpoints ====================

    @GetMapping("/{bomId}/lines")
    public ResponseEntity<List<BomLine>> getBomLines(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<BomLine> lines = bomService.getBomLines(bomId);
        return ResponseEntity.ok(lines);
    }

    @GetMapping("/{bomId}/lines/top-level")
    public ResponseEntity<List<BomLine>> getTopLevelComponents(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<BomLine> lines = bomService.getTopLevelComponents(bomId);
        return ResponseEntity.ok(lines);
    }

    @PostMapping("/{bomId}/lines")
    public ResponseEntity<BomLine> addBomLine(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId,
            @RequestBody BomLine bomLine) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomLine created = bomService.addBomLine(bomLine);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/lines/{bomLineId}")
    public ResponseEntity<BomLine> updateBomLine(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomLineId,
            @RequestBody BomLine bomLine) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getOrganizationIdForBomLine(bomLineId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomLine updated = bomService.updateBomLine(bomLineId, bomLine);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/lines/{bomLineId}")
    public ResponseEntity<Void> deleteBomLine(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomLineId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getOrganizationIdForBomLine(bomLineId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        bomService.deleteBomLine(bomLineId);
        return ResponseEntity.noContent().build();
    }

    // ==================== BOM Explosion ====================

    @GetMapping("/{bomId}/explosion")
    public ResponseEntity<Map<String, Object>> explodeBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId,
            @RequestParam(defaultValue = "1") BigDecimal quantity) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        Map<String, Object> explosion = bomService.explodeBom(bomId, quantity);
        return ResponseEntity.ok(explosion);
    }

    // ==================== BOM Version Endpoints ====================

    @GetMapping("/{bomId}/versions")
    public ResponseEntity<List<BomVersion>> getBomVersions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<BomVersion> versions = bomService.getBomVersions(bomId);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/{bomId}/versions")
    public ResponseEntity<BomVersion> createBomVersion(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId,
            @RequestBody BomVersion bomVersion) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        BomVersion created = bomService.createBomVersion(bomVersion);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== Dashboard & Stats ====================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        Map<String, Object> stats = bomService.getBomDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
