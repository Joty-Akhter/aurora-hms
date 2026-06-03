package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.WorkOrder;
import com.easyops.manufacturing.entity.WorkOrderMaterial;
import com.easyops.manufacturing.entity.WorkOrderOperation;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/work-orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final ManufacturingRbacService manufacturingRbac;

    // ==================== Work Order CRUD ====================

    @GetMapping
    public ResponseEntity<List<WorkOrder>> getAllWorkOrders(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkOrder> workOrders = workOrderService.getAllWorkOrders(organizationId);
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/{workOrderId}")
    public ResponseEntity<WorkOrder> getWorkOrderById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return workOrderService.getWorkOrderById(workOrderId)
                .map(wo -> {
                    manufacturingRbac.requireManufacturingView(actor, wo.getOrganizationId());
                    return ResponseEntity.ok(wo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{workOrderNumber}")
    public ResponseEntity<WorkOrder> getWorkOrderByNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String workOrderNumber,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        return workOrderService.getWorkOrderByNumber(organizationId, workOrderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersByStatus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String status,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkOrder> workOrders = workOrderService.getWorkOrdersByStatus(organizationId, status);
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/active")
    public ResponseEntity<List<WorkOrder>> getActiveWorkOrders(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkOrder> workOrders = workOrderService.getActiveWorkOrders(organizationId);
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<WorkOrder>> getOverdueWorkOrders(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<WorkOrder> workOrders = workOrderService.getOverdueWorkOrders(organizationId);
        return ResponseEntity.ok(workOrders);
    }

    @PostMapping
    public ResponseEntity<WorkOrder> createWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody WorkOrder workOrder) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, workOrder.getOrganizationId());
        WorkOrder created = workOrderService.createWorkOrder(workOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{workOrderId}")
    public ResponseEntity<WorkOrder> updateWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId,
            @RequestBody WorkOrder workOrder) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder updated = workOrderService.updateWorkOrder(workOrderId, workOrder);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{workOrderId}")
    public ResponseEntity<Void> deleteWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.deleteWorkOrder(workOrderId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Work Order Lifecycle ====================

    @PostMapping("/{workOrderId}/release")
    public ResponseEntity<WorkOrder> releaseWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId,
            @RequestParam UUID releasedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder released = workOrderService.releaseWorkOrder(workOrderId, releasedBy);
        return ResponseEntity.ok(released);
    }

    @PostMapping("/{workOrderId}/start")
    public ResponseEntity<WorkOrder> startWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder started = workOrderService.startWorkOrder(workOrderId, userId);
        return ResponseEntity.ok(started);
    }

    @PostMapping("/{workOrderId}/complete")
    public ResponseEntity<WorkOrder> completeWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId,
            @RequestParam UUID completedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder completed = workOrderService.completeWorkOrder(workOrderId, completedBy);
        return ResponseEntity.ok(completed);
    }

    @PostMapping("/{workOrderId}/close")
    public ResponseEntity<WorkOrder> closeWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder closed = workOrderService.closeWorkOrder(workOrderId);
        return ResponseEntity.ok(closed);
    }

    @PostMapping("/{workOrderId}/cancel")
    public ResponseEntity<WorkOrder> cancelWorkOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId,
            @RequestParam String reason) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrder cancelled = workOrderService.cancelWorkOrder(workOrderId, reason);
        return ResponseEntity.ok(cancelled);
    }

    // ==================== Material Management ====================

    @PostMapping("/{workOrderId}/materials/reserve")
    public ResponseEntity<Void> reserveMaterials(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.reserveMaterials(workOrderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workOrderId}/materials/unreserve")
    public ResponseEntity<Void> unreserveMaterials(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.unreserveMaterials(workOrderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/materials/{materialId}/issue")
    public ResponseEntity<WorkOrderMaterial> issueMaterial(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID materialId,
            @RequestParam BigDecimal quantity,
            @RequestParam UUID issuedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrderMaterial(materialId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrderMaterial issued = workOrderService.issueMaterial(materialId, quantity, issuedBy);
        return ResponseEntity.ok(issued);
    }

    @PostMapping("/materials/{materialId}/consume")
    public ResponseEntity<WorkOrderMaterial> consumeMaterial(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID materialId,
            @RequestParam BigDecimal quantity) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrderMaterial(materialId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrderMaterial consumed = workOrderService.consumeMaterial(materialId, quantity);
        return ResponseEntity.ok(consumed);
    }

    @PostMapping("/{workOrderId}/materials/backflush")
    public ResponseEntity<Void> backflushMaterials(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.backflushMaterials(workOrderId);
        return ResponseEntity.ok().build();
    }

    // ==================== Operation Management ====================

    @GetMapping("/{workOrderId}/operations")
    public ResponseEntity<List<WorkOrderOperation>> getWorkOrderOperations(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<WorkOrderOperation> operations = workOrderService.getWorkOrderOperations(workOrderId);
        return ResponseEntity.ok(operations);
    }

    @PostMapping("/operations/{operationId}/start")
    public ResponseEntity<WorkOrderOperation> startOperation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID operationId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrderOperation(operationId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrderOperation started = workOrderService.startOperation(operationId, userId);
        return ResponseEntity.ok(started);
    }

    @PostMapping("/operations/{operationId}/complete")
    public ResponseEntity<WorkOrderOperation> completeOperation(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID operationId,
            @RequestParam BigDecimal quantityCompleted,
            @RequestParam UUID completedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrderOperation(operationId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        WorkOrderOperation completed = workOrderService.completeOperation(operationId, quantityCompleted, completedBy);
        return ResponseEntity.ok(completed);
    }

    // ==================== Progress & Cost Management ====================

    @PostMapping("/{workOrderId}/progress/update")
    public ResponseEntity<Void> updateProgress(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.updateWorkOrderProgress(workOrderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workOrderId}/costs/recalculate")
    public ResponseEntity<Void> recalculateCosts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID workOrderId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = workOrderService.getOrganizationIdForWorkOrder(workOrderId);
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        workOrderService.recalculateWorkOrderCosts(workOrderId);
        return ResponseEntity.ok().build();
    }

    // ==================== Dashboard & Stats ====================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        Map<String, Object> stats = workOrderService.getDashboardStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
