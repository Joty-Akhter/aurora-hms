package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.EquipmentMaintenance;
import com.easyops.manufacturing.entity.WorkCenter;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.WorkCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/work-centers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkCenterController {

    private final WorkCenterService workCenterService;
    private final ManufacturingRbacService manufacturingRbac;

    // ==================== Work Center Endpoints ====================

    @GetMapping
    public ResponseEntity<List<WorkCenter>> getAllWorkCenters(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkCenter> workCenters = workCenterService.getAllWorkCenters(organizationId);
        return ResponseEntity.ok(workCenters);
    }

    @GetMapping("/{workCenterId}")
    public ResponseEntity<WorkCenter> getWorkCenterById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workCenterId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return workCenterService.getWorkCenterById(workCenterId)
                .map(wc -> {
                    manufacturingRbac.requireManufacturingView(actor, wc.getOrganizationId());
                    return ResponseEntity.ok(wc);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{workCenterCode}")
    public ResponseEntity<WorkCenter> getWorkCenterByCode(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String workCenterCode,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        return workCenterService.getWorkCenterByCode(organizationId, workCenterCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<WorkCenter>> getActiveWorkCenters(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkCenter> workCenters = workCenterService.getActiveWorkCenters(organizationId);
        return ResponseEntity.ok(workCenters);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<WorkCenter>> getWorkCentersByType(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String type,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkCenter> workCenters = workCenterService.getWorkCentersByType(organizationId, type);
        return ResponseEntity.ok(workCenters);
    }

    @PostMapping
    public ResponseEntity<WorkCenter> createWorkCenter(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody WorkCenter workCenter) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, workCenter.getOrganizationId());
        WorkCenter created = workCenterService.createWorkCenter(workCenter);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{workCenterId}")
    public ResponseEntity<WorkCenter> updateWorkCenter(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workCenterId,
            @RequestBody WorkCenter workCenter) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getWorkCenterById(workCenterId)
                .map(WorkCenter::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work center not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkCenter updated = workCenterService.updateWorkCenter(workCenterId, workCenter);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{workCenterId}")
    public ResponseEntity<Void> deleteWorkCenter(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workCenterId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getWorkCenterById(workCenterId)
                .map(WorkCenter::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work center not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workCenterService.deleteWorkCenter(workCenterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workCenterId}/status")
    public ResponseEntity<WorkCenter> updateWorkCenterStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workCenterId,
            @RequestParam String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getWorkCenterById(workCenterId)
                .map(WorkCenter::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work center not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkCenter updated = workCenterService.updateWorkCenterStatus(workCenterId, status);
        return ResponseEntity.ok(updated);
    }

    // ==================== Equipment Maintenance Endpoints ====================

    @GetMapping("/{workCenterId}/maintenance")
    public ResponseEntity<List<EquipmentMaintenance>> getMaintenanceByWorkCenter(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workCenterId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getWorkCenterById(workCenterId)
                .map(WorkCenter::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work center not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<EquipmentMaintenance> maintenance = workCenterService.getMaintenanceByWorkCenter(workCenterId);
        return ResponseEntity.ok(maintenance);
    }

    @GetMapping("/maintenance")
    public ResponseEntity<List<EquipmentMaintenance>> getAllMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<EquipmentMaintenance> maintenance = workCenterService.getAllMaintenance(organizationId);
        return ResponseEntity.ok(maintenance);
    }

    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<EquipmentMaintenance> getMaintenanceById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID maintenanceId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return workCenterService.getMaintenanceById(maintenanceId)
                .map(m -> {
                    manufacturingRbac.requireManufacturingView(actor, m.getOrganizationId());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/maintenance/overdue")
    public ResponseEntity<List<EquipmentMaintenance>> getOverdueMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<EquipmentMaintenance> maintenance = workCenterService.getOverdueMaintenance(organizationId);
        return ResponseEntity.ok(maintenance);
    }

    @PostMapping("/maintenance")
    public ResponseEntity<EquipmentMaintenance> createMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EquipmentMaintenance maintenance) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, maintenance.getOrganizationId());
        EquipmentMaintenance created = workCenterService.createMaintenance(maintenance);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<EquipmentMaintenance> updateMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID maintenanceId,
            @RequestBody EquipmentMaintenance maintenance) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getMaintenanceById(maintenanceId)
                .map(EquipmentMaintenance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        EquipmentMaintenance updated = workCenterService.updateMaintenance(maintenanceId, maintenance);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<Void> deleteMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID maintenanceId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getMaintenanceById(maintenanceId)
                .map(EquipmentMaintenance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workCenterService.deleteMaintenance(maintenanceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/maintenance/{maintenanceId}/start")
    public ResponseEntity<EquipmentMaintenance> startMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID maintenanceId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getMaintenanceById(maintenanceId)
                .map(EquipmentMaintenance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        EquipmentMaintenance started = workCenterService.startMaintenance(maintenanceId, userId);
        return ResponseEntity.ok(started);
    }

    @PostMapping("/maintenance/{maintenanceId}/complete")
    public ResponseEntity<EquipmentMaintenance> completeMaintenance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID maintenanceId,
            @RequestParam UUID completedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workCenterService.getMaintenanceById(maintenanceId)
                .map(EquipmentMaintenance::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        EquipmentMaintenance completed = workCenterService.completeMaintenance(maintenanceId, completedBy);
        return ResponseEntity.ok(completed);
    }

    // Dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        Map<String, Object> stats = workCenterService.getWorkCenterDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
