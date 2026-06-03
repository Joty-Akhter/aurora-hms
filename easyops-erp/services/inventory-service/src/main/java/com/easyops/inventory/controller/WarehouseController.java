package com.easyops.inventory.controller;

import com.easyops.inventory.entity.Warehouse;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.WarehouseService;
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
@RequestMapping("/api/inventory/warehouses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Warehouse Management", description = "Warehouse management APIs")
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/warehouses - organizationId: {}, activeOnly: {}", organizationId, activeOnly);
        List<Warehouse> warehouses = Boolean.TRUE.equals(activeOnly)
                ? warehouseService.getActiveWarehouses(organizationId)
                : warehouseService.getAllWarehouses(organizationId);
        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<Warehouse> getWarehouseById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Warehouse warehouse = warehouseService.getWarehouseById(id);
        inventoryRbac.requireInventoryView(actor, warehouse.getOrganizationId());
        log.info("GET /api/inventory/warehouses/{}", id);
        return ResponseEntity.ok(warehouse);
    }

    @PostMapping
    @Operation(summary = "Create new warehouse")
    public ResponseEntity<Warehouse> createWarehouse(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Warehouse warehouse) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, warehouse.getOrganizationId());
        log.info("POST /api/inventory/warehouses - code: {}", warehouse.getCode());
        Warehouse createdWarehouse = warehouseService.createWarehouse(warehouse);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWarehouse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse")
    public ResponseEntity<Warehouse> updateWarehouse(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody Warehouse warehouse) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Warehouse existing = warehouseService.getWarehouseById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("PUT /api/inventory/warehouses/{}", id);
        Warehouse updatedWarehouse = warehouseService.updateWarehouse(id, warehouse);
        return ResponseEntity.ok(updatedWarehouse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete warehouse")
    public ResponseEntity<Void> deleteWarehouse(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Warehouse existing = warehouseService.getWarehouseById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/inventory/warehouses/{}", id);
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
