package com.easyops.pharma.controller;

import com.easyops.pharma.entity.*;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.TerritoryAnalyticsService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/territories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Territory Management", description = "Territory hierarchy management APIs")
@CrossOrigin(origins = "*")
public class TerritoryController {

    private final TerritoryService territoryService;
    private final TerritoryAnalyticsService territoryAnalyticsService;
    private final PharmaRbacService pharmaRbac;

    // Division endpoints
    @GetMapping("/divisions")
    @Operation(summary = "Get all divisions")
    public ResponseEntity<List<Division>> getAllDivisions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/territories/divisions - organizationId: {}", organizationId);
        List<Division> divisions = territoryService.getAllDivisions(organizationId);
        return ResponseEntity.ok(divisions);
    }

    @GetMapping("/divisions/active")
    @Operation(summary = "Get active divisions")
    public ResponseEntity<List<Division>> getActiveDivisions(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/territories/divisions/active - organizationId: {}", organizationId);
        List<Division> divisions = territoryService.getActiveDivisions(organizationId);
        return ResponseEntity.ok(divisions);
    }

    @GetMapping("/divisions/{id}")
    @Operation(summary = "Get division by ID")
    public ResponseEntity<Division> getDivisionById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Division division = territoryService.getDivisionById(id);
        pharmaRbac.requirePharmaView(actor, division.getOrganizationId());
        log.info("GET /api/pharma/territories/divisions/{}", id);
        return ResponseEntity.ok(division);
    }

    @PostMapping("/divisions")
    @Operation(summary = "Create new division")
    public ResponseEntity<Division> createDivision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Division division) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, division.getOrganizationId());
        log.info("POST /api/pharma/territories/divisions - name: {}", division.getName());
        Division created = territoryService.createDivision(division);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/divisions/{id}")
    @Operation(summary = "Update division")
    public ResponseEntity<Division> updateDivision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Division division) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, division.getOrganizationId());
        log.info("PUT /api/pharma/territories/divisions/{}", id);
        Division updated = territoryService.updateDivision(id, division);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/divisions/{id}")
    @Operation(summary = "Delete division")
    public ResponseEntity<Void> deleteDivision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Division existing = territoryService.getDivisionById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/territories/divisions/{}", id);
        territoryService.deleteDivision(id);
        return ResponseEntity.noContent().build();
    }

    // Region endpoints
    @GetMapping("/regions")
    @Operation(summary = "Get regions by division")
    public ResponseEntity<List<Region>> getRegionsByDivision(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("divisionId") UUID divisionId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Division division = territoryService.getDivisionById(divisionId);
        pharmaRbac.requirePharmaView(actor, division.getOrganizationId());
        log.info("GET /api/pharma/territories/regions - divisionId: {}", divisionId);
        List<Region> regions = territoryService.getRegionsByDivision(divisionId);
        return ResponseEntity.ok(regions);
    }

    @GetMapping("/regions/{id}")
    @Operation(summary = "Get region by ID")
    public ResponseEntity<Region> getRegionById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Region region = territoryService.getRegionById(id);
        pharmaRbac.requirePharmaView(actor, region.getOrganizationId());
        log.info("GET /api/pharma/territories/regions/{}", id);
        return ResponseEntity.ok(region);
    }

    @PostMapping("/regions")
    @Operation(summary = "Create new region")
    public ResponseEntity<Region> createRegion(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Region region) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, region.getOrganizationId());
        log.info("POST /api/pharma/territories/regions - name: {}", region.getName());
        Region created = territoryService.createRegion(region);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/regions/{id}")
    @Operation(summary = "Update region")
    public ResponseEntity<Region> updateRegion(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Region region) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, region.getOrganizationId());
        log.info("PUT /api/pharma/territories/regions/{}", id);
        Region updated = territoryService.updateRegion(id, region);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/regions/{id}")
    @Operation(summary = "Delete region")
    public ResponseEntity<Void> deleteRegion(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Region existing = territoryService.getRegionById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/territories/regions/{}", id);
        territoryService.deleteRegion(id);
        return ResponseEntity.noContent().build();
    }

    // Territory endpoints
    @GetMapping("/territories")
    @Operation(summary = "Get territories by region or area")
    public ResponseEntity<List<Territory>> getTerritories(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "areaId", required = false) UUID areaId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (areaId != null) {
            Area area = territoryService.getAreaById(areaId);
            pharmaRbac.requirePharmaView(actor, area.getOrganizationId());
            log.info("GET /api/pharma/territories/territories - areaId: {}", areaId);
            List<Territory> territories = territoryService.getTerritoriesByArea(areaId);
            return ResponseEntity.ok(territories);
        }
        if (regionId != null) {
            Region region = territoryService.getRegionById(regionId);
            pharmaRbac.requirePharmaView(actor, region.getOrganizationId());
            log.info("GET /api/pharma/territories/territories - regionId: {}", regionId);
            List<Territory> territories = territoryService.getTerritoriesByRegion(regionId);
            return ResponseEntity.ok(territories);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/territories/{id}")
    @Operation(summary = "Get territory by ID")
    public ResponseEntity<Territory> getTerritoryById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Territory territory = territoryService.getTerritoryById(id);
        pharmaRbac.requirePharmaView(actor, territory.getOrganizationId());
        log.info("GET /api/pharma/territories/territories/{}", id);
        return ResponseEntity.ok(territory);
    }

    @PostMapping("/territories")
    @Operation(summary = "Create new territory")
    public ResponseEntity<Territory> createTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Territory territory) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, territory.getOrganizationId());
        log.info("POST /api/pharma/territories/territories - name: {}", territory.getName());
        Territory created = territoryService.createTerritory(territory);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/territories/{id}")
    @Operation(summary = "Update territory")
    public ResponseEntity<Territory> updateTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Territory territory) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, territory.getOrganizationId());
        log.info("PUT /api/pharma/territories/territories/{}", id);
        Territory updated = territoryService.updateTerritory(id, territory);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/territories/{id}")
    @Operation(summary = "Delete territory")
    public ResponseEntity<Void> deleteTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Territory existing = territoryService.getTerritoryById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/territories/territories/{}", id);
        territoryService.deleteTerritory(id);
        return ResponseEntity.noContent().build();
    }

    // Area endpoints
    @GetMapping("/areas")
    @Operation(summary = "Get areas by region, territory, or all areas for organization")
    public ResponseEntity<List<Area>> getAreas(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "territoryId", required = false) UUID territoryId,
            @RequestParam(name = "organizationId", required = false) UUID organizationId,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (regionId != null) {
            Region region = territoryService.getRegionById(regionId);
            pharmaRbac.requirePharmaView(actor, region.getOrganizationId());
            log.info("GET /api/pharma/territories/areas - regionId: {}", regionId);
            List<Area> areas = territoryService.getAreasByRegion(regionId);
            return ResponseEntity.ok(areas);
        }
        if (territoryId != null) {
            Territory territory = territoryService.getTerritoryById(territoryId);
            pharmaRbac.requirePharmaView(actor, territory.getOrganizationId());
            log.info("GET /api/pharma/territories/areas - territoryId: {}", territoryId);
            List<Area> areas = territoryService.getAreasByTerritory(territoryId);
            return ResponseEntity.ok(areas);
        }
        if (organizationId != null) {
            pharmaRbac.requirePharmaView(actor, organizationId);
            log.info("GET /api/pharma/territories/areas - organizationId: {}, includeInactive: {}", organizationId, includeInactive);
            List<Area> areas = includeInactive
                    ? territoryService.getAllAreasByOrganization(organizationId)
                    : territoryService.getAllActiveAreas(organizationId);
            return ResponseEntity.ok(areas);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/areas/{id}")
    @Operation(summary = "Get area by ID")
    public ResponseEntity<Area> getAreaById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Area area = territoryService.getAreaById(id);
        pharmaRbac.requirePharmaView(actor, area.getOrganizationId());
        log.info("GET /api/pharma/territories/areas/{}", id);
        return ResponseEntity.ok(area);
    }

    @PostMapping("/areas")
    @Operation(summary = "Create new area")
    public ResponseEntity<Area> createArea(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Area area) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, area.getOrganizationId());
        log.info("POST /api/pharma/territories/areas - name: {}", area.getName());
        Area created = territoryService.createArea(area);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/areas/{id}")
    @Operation(summary = "Update area")
    public ResponseEntity<Area> updateArea(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Area area) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, area.getOrganizationId());
        log.info("PUT /api/pharma/territories/areas/{}", id);
        Area updated = territoryService.updateArea(id, area);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/areas/{id}")
    @Operation(summary = "Delete area")
    public ResponseEntity<Void> deleteArea(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Area existing = territoryService.getAreaById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/territories/areas/{}", id);
        territoryService.deleteArea(id);
        return ResponseEntity.noContent().build();
    }

    // Territory Analytics endpoints (Phase 5)
    @GetMapping("/territories/{id}/analytics")
    @Operation(summary = "Get territory performance analytics", description = "Phase 5: Advanced territory analytics")
    public ResponseEntity<Map<String, Object>> getTerritoryAnalytics(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Territory territory = territoryService.getTerritoryById(id);
        pharmaRbac.requirePharmaView(actor, territory.getOrganizationId());
        log.info("GET /api/pharma/territories/territories/{}/analytics - year: {}, month: {}", id, year, month);
        Map<String, Object> analytics = territoryAnalyticsService.getTerritoryPerformanceAnalytics(id, year, month);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/territories/{id}/optimization")
    @Operation(summary = "Get territory optimization recommendations", description = "Phase 5: Territory optimization suggestions")
    public ResponseEntity<Map<String, Object>> getTerritoryOptimization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Territory territory = territoryService.getTerritoryById(id);
        pharmaRbac.requirePharmaView(actor, territory.getOrganizationId());
        log.info("GET /api/pharma/territories/territories/{}/optimization", id);
        Map<String, Object> recommendations = territoryAnalyticsService.getTerritoryOptimizationRecommendations(id);
        return ResponseEntity.ok(recommendations);
    }
}
