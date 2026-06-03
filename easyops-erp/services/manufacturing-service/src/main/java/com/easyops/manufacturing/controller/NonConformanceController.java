package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.NonConformance;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.NonConformanceService;
import com.easyops.manufacturing.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/quality/non-conformances")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NonConformanceController {

    private final NonConformanceService nonConformanceService;
    private final WorkOrderService workOrderService;
    private final ManufacturingRbacService manufacturingRbac;

    @GetMapping
    public ResponseEntity<List<NonConformance>> getAllNonConformances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<NonConformance> ncs = nonConformanceService.getAllNonConformances(organizationId);
        return ResponseEntity.ok(ncs);
    }

    @GetMapping("/{ncId}")
    public ResponseEntity<NonConformance> getNonConformanceById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID ncId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return nonConformanceService.getNonConformanceById(ncId)
                .map(nc -> {
                    manufacturingRbac.requireManufacturingView(actor, nc.getOrganizationId());
                    return ResponseEntity.ok(nc);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<NonConformance>> getNonConformancesByStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String status,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<NonConformance> ncs = nonConformanceService.getNonConformancesByStatus(organizationId, status);
        return ResponseEntity.ok(ncs);
    }

    @GetMapping("/open")
    public ResponseEntity<List<NonConformance>> getOpenNonConformances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<NonConformance> ncs = nonConformanceService.getOpenNonConformances(organizationId);
        return ResponseEntity.ok(ncs);
    }

    @GetMapping("/critical")
    public ResponseEntity<List<NonConformance>> getCriticalOpenNCs(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<NonConformance> ncs = nonConformanceService.getCriticalOpenNCs(organizationId);
        return ResponseEntity.ok(ncs);
    }

    @GetMapping("/work-order/{workOrderId}")
    public ResponseEntity<List<NonConformance>> getNonConformancesByWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<NonConformance> ncs = nonConformanceService.getNonConformancesByWorkOrder(workOrderId);
        return ResponseEntity.ok(ncs);
    }

    @PostMapping
    public ResponseEntity<NonConformance> createNonConformance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody NonConformance nc) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, nc.getOrganizationId());
        NonConformance created = nonConformanceService.createNonConformance(nc);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{ncId}")
    public ResponseEntity<NonConformance> updateNonConformance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID ncId,
            @RequestBody NonConformance nc) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = nonConformanceService.getNonConformanceById(ncId)
                .map(NonConformance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non-conformance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        NonConformance updated = nonConformanceService.updateNonConformance(ncId, nc);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{ncId}")
    public ResponseEntity<Void> deleteNonConformance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID ncId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = nonConformanceService.getNonConformanceById(ncId)
                .map(NonConformance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non-conformance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        nonConformanceService.deleteNonConformance(ncId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ncId}/assign")
    public ResponseEntity<NonConformance> assignNonConformance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID ncId,
            @RequestParam UUID assignedTo) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = nonConformanceService.getNonConformanceById(ncId)
                .map(NonConformance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non-conformance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        NonConformance assigned = nonConformanceService.assignNonConformance(ncId, assignedTo);
        return ResponseEntity.ok(assigned);
    }

    @PostMapping("/{ncId}/resolve")
    public ResponseEntity<NonConformance> resolveNonConformance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID ncId,
            @RequestParam String disposition,
            @RequestParam String resolutionNotes,
            @RequestParam UUID resolvedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = nonConformanceService.getNonConformanceById(ncId)
                .map(NonConformance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non-conformance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        NonConformance resolved = nonConformanceService.resolveNonConformance(ncId, disposition, resolutionNotes, resolvedBy);
        return ResponseEntity.ok(resolved);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        Map<String, Object> stats = nonConformanceService.getNonConformanceDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
