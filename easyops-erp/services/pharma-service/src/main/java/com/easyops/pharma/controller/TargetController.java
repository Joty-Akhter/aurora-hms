package com.easyops.pharma.controller;

import com.easyops.pharma.entity.Target;
import com.easyops.pharma.entity.TargetCoverage;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.TargetService;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/targets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Target Management", description = "Territory-wise target setting and tracking APIs")
@CrossOrigin(origins = "*")
public class TargetController {

    private final TargetService targetService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all targets")
    public ResponseEntity<List<Target>> getAllTargets(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/targets - organizationId: {}", organizationId);
        List<Target> targets = targetService.getAllTargets(organizationId);
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get targets by territory")
    public ResponseEntity<List<Target>> getTargetsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/targets/territory/{}", territoryId);
        List<Target> targets = targetService.getTargetsByTerritory(territoryId);
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get targets by employee")
    public ResponseEntity<List<Target>> getTargetsByEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeId") UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        List<Target> targets = targetService.getTargetsByEmployee(employeeId);
        UUID orgId = targets.stream().findFirst().map(Target::getOrganizationId).orElse(null);
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/targets/employee/{}", employeeId);
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/territory/{territoryId}/month")
    @Operation(summary = "Get active target for territory and month")
    public ResponseEntity<Target> getActiveTargetForTerritoryAndMonth(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/targets/territory/{}/month - year: {}, month: {}", territoryId, year, month);
        Optional<Target> target = targetService.getActiveTargetForTerritoryAndMonth(territoryId, year, month);
        return target.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get target by ID")
    public ResponseEntity<Target> getTargetById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Target target = targetService.getTargetById(id);
        pharmaRbac.requirePharmaView(actor, target.getOrganizationId());
        log.info("GET /api/pharma/targets/{}", id);
        return ResponseEntity.ok(target);
    }

    @PostMapping
    @Operation(summary = "Create new target")
    public ResponseEntity<Target> createTarget(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Target target) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, target.getOrganizationId());
        log.info("POST /api/pharma/targets");
        Target created = targetService.createTarget(target);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update target")
    public ResponseEntity<Target> updateTarget(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Target target) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, target.getOrganizationId());
        log.info("PUT /api/pharma/targets/{}", id);
        Target updated = targetService.updateTarget(id, target);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete target")
    public ResponseEntity<Void> deleteTarget(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Target existing = targetService.getTargetById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/targets/{}", id);
        targetService.deleteTarget(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/coverage/calculate")
    @Operation(summary = "Calculate target coverage")
    public ResponseEntity<TargetCoverage> calculateCoverage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaManage(actor, orgId);
        log.info("POST /api/pharma/targets/coverage/calculate - territory: {}, year: {}, month: {}", territoryId, year, month);
        TargetCoverage coverage = targetService.calculateCoverage(territoryId, year, month);
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/coverage/territory/{territoryId}")
    @Operation(summary = "Get coverage by territory and year")
    public ResponseEntity<List<TargetCoverage>> getCoverageByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/targets/coverage/territory/{} - year: {}", territoryId, year);
        List<TargetCoverage> coverage = targetService.getCoverageByTerritory(territoryId, year);
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/coverage/territory/{territoryId}/month")
    @Operation(summary = "Get coverage by territory and month")
    public ResponseEntity<TargetCoverage> getCoverageByTerritoryAndMonth(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/targets/coverage/territory/{}/month - year: {}, month: {}", territoryId, year, month);
        Optional<TargetCoverage> coverage = targetService.getCoverageByTerritoryAndMonth(territoryId, year, month);
        return coverage.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
