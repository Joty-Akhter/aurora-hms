package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.QualityInspection;
import com.easyops.manufacturing.entity.QualityInspectionItem;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.QualityService;
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
@RequestMapping("/quality/inspections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QualityController {

    private final QualityService qualityService;
    private final WorkOrderService workOrderService;
    private final ManufacturingRbacService manufacturingRbac;

    @GetMapping
    public ResponseEntity<List<QualityInspection>> getAllInspections(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<QualityInspection> inspections = qualityService.getAllInspections(organizationId);
        return ResponseEntity.ok(inspections);
    }

    @GetMapping("/{inspectionId}")
    public ResponseEntity<QualityInspection> getInspectionById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return qualityService.getInspectionById(inspectionId)
                .map(insp -> {
                    manufacturingRbac.requireManufacturingView(actor, insp.getOrganizationId());
                    return ResponseEntity.ok(insp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<QualityInspection>> getInspectionsByStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String status,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<QualityInspection> inspections = qualityService.getInspectionsByStatus(organizationId, status);
        return ResponseEntity.ok(inspections);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<QualityInspection>> getInspectionsByProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<QualityInspection> inspections = qualityService.getInspectionsByProduct(organizationId, productId);
        return ResponseEntity.ok(inspections);
    }

    @GetMapping("/work-order/{workOrderId}")
    public ResponseEntity<List<QualityInspection>> getInspectionsByWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<QualityInspection> inspections = qualityService.getInspectionsByWorkOrder(workOrderId);
        return ResponseEntity.ok(inspections);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<QualityInspection>> getPendingInspections(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<QualityInspection> inspections = qualityService.getPendingInspections(organizationId);
        return ResponseEntity.ok(inspections);
    }

    @PostMapping
    public ResponseEntity<QualityInspection> createInspection(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody QualityInspection inspection) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, inspection.getOrganizationId());
        QualityInspection created = qualityService.createInspection(inspection);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{inspectionId}")
    public ResponseEntity<QualityInspection> updateInspection(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId,
            @RequestBody QualityInspection inspection) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getInspectionById(inspectionId)
                .map(QualityInspection::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        QualityInspection updated = qualityService.updateInspection(inspectionId, inspection);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{inspectionId}")
    public ResponseEntity<Void> deleteInspection(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getInspectionById(inspectionId)
                .map(QualityInspection::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        qualityService.deleteInspection(inspectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{inspectionId}/complete")
    public ResponseEntity<QualityInspection> completeInspection(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId,
            @RequestParam String overallResult,
            @RequestParam UUID completedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getInspectionById(inspectionId)
                .map(QualityInspection::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        QualityInspection completed = qualityService.completeInspection(inspectionId, overallResult, completedBy);
        return ResponseEntity.ok(completed);
    }

    // Inspection Items
    @GetMapping("/{inspectionId}/items")
    public ResponseEntity<List<QualityInspectionItem>> getInspectionItems(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getInspectionById(inspectionId)
                .map(QualityInspection::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<QualityInspectionItem> items = qualityService.getInspectionItems(inspectionId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{inspectionId}/items")
    public ResponseEntity<QualityInspectionItem> addInspectionItem(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID inspectionId,
            @RequestBody QualityInspectionItem item) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getInspectionById(inspectionId)
                .map(QualityInspection::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        QualityInspectionItem created = qualityService.addInspectionItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<QualityInspectionItem> updateInspectionItem(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID itemId,
            @RequestBody QualityInspectionItem item) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getOrganizationIdForInspectionItem(itemId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        QualityInspectionItem updated = qualityService.updateInspectionItem(itemId, item);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteInspectionItem(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID itemId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = qualityService.getOrganizationIdForInspectionItem(itemId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        qualityService.deleteInspectionItem(itemId);
        return ResponseEntity.noContent().build();
    }

    // Dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        Map<String, Object> stats = qualityService.getQualityDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
