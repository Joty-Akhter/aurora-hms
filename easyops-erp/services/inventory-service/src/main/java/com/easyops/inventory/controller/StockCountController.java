package com.easyops.inventory.controller;

import com.easyops.inventory.entity.StockCount;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.StockCountService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/stock-counts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stock Counting", description = "Physical inventory count and cycle counting APIs")
@CrossOrigin(origins = "*")
public class StockCountController {

    private final StockCountService stockCountService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping
    @Operation(summary = "Get all stock counts")
    public ResponseEntity<List<StockCount>> getAllCounts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID warehouseId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock-counts - org: {}, status: {}, warehouse: {}",
                organizationId, status, warehouseId);

        List<StockCount> counts;
        if (status != null) {
            counts = stockCountService.getCountsByStatus(organizationId, status);
        } else if (warehouseId != null) {
            counts = stockCountService.getCountsByWarehouse(organizationId, warehouseId);
        } else {
            counts = stockCountService.getAllCounts(organizationId);
        }

        return ResponseEntity.ok(counts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock count by ID with lines")
    public ResponseEntity<StockCount> getCountById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount count = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryView(actor, count.getOrganizationId());
        log.info("GET /api/inventory/stock-counts/{}", id);
        return ResponseEntity.ok(count);
    }

    @PostMapping
    @Operation(summary = "Create new stock count")
    public ResponseEntity<StockCount> createCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockCount count) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, count.getOrganizationId());
        log.info("POST /api/inventory/stock-counts - number: {}", count.getCountNumber());
        StockCount created = stockCountService.createCount(count);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start stock count (generates count lines)")
    public ResponseEntity<StockCount> startCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount existing = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/stock-counts/{}/start", id);
        StockCount count = stockCountService.startCount(id, userId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/record")
    @Operation(summary = "Record counted quantity for a product")
    public ResponseEntity<StockCount> recordCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount existing = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/stock-counts/{}/record", id);

        UUID productId = UUID.fromString(request.get("productId").toString());
        BigDecimal countedQty = new BigDecimal(request.get("countedQuantity").toString());
        UUID userId = UUID.fromString(request.get("userId").toString());

        StockCount count = stockCountService.recordCount(id, productId, countedQty, userId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete stock count")
    public ResponseEntity<StockCount> completeCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount existing = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/stock-counts/{}/complete", id);
        StockCount count = stockCountService.completeCount(id);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve stock count and apply adjustments")
    public ResponseEntity<StockCount> approveCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestParam UUID approvedBy) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount existing = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/stock-counts/{}/approve - by: {}", id, approvedBy);
        StockCount count = stockCountService.approveCount(id, approvedBy);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel stock count")
    public ResponseEntity<Void> cancelCount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        StockCount existing = stockCountService.getCountById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/stock-counts/{}/cancel", id);
        stockCountService.cancelCount(id);
        return ResponseEntity.noContent().build();
    }
}
