package com.easyops.inventory.controller;

import com.easyops.inventory.dto.InventoryReceiveRequest;
import com.easyops.inventory.dto.StockAllocationRequest;
import com.easyops.inventory.dto.StockAdjustmentRequest;
import com.easyops.inventory.dto.StockIssueRequest;
import com.easyops.inventory.dto.StockReceiptRequest;
import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/stock")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stock Management", description = "Stock level and movement management APIs")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping
    @Operation(summary = "Get stock levels")
    public ResponseEntity<List<Stock>> getStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "warehouseId", required = false) UUID warehouseId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock - org: {}, product: {}, warehouse: {}", organizationId, productId, warehouseId);

        List<Stock> stock;
        if (productId != null) {
            stock = stockService.getStockByProduct(organizationId, productId);
        } else if (warehouseId != null) {
            stock = stockService.getStockByWarehouse(organizationId, warehouseId);
        } else {
            stock = stockService.getStockByOrganization(organizationId);
        }

        return ResponseEntity.ok(stock);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available quantity for product in warehouse")
    public ResponseEntity<Map<String, BigDecimal>> getAvailableQuantity(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("productId") UUID productId,
            @RequestParam("warehouseId") UUID warehouseId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock/available - org: {}, product: {}, warehouse: {}", organizationId, productId, warehouseId);
        BigDecimal quantity = stockService.getAvailableQuantity(productId, warehouseId);
        return ResponseEntity.ok(Map.of("availableQuantity", quantity));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items")
    public ResponseEntity<List<Stock>> getLowStockItems(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock/low-stock - org: {}", organizationId);
        List<Stock> lowStock = stockService.getLowStockItems(organizationId);
        return ResponseEntity.ok(lowStock);
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "Get out of stock items")
    public ResponseEntity<List<Stock>> getOutOfStockItems(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock/out-of-stock - org: {}", organizationId);
        List<Stock> outOfStock = stockService.getOutOfStockItems(organizationId);
        return ResponseEntity.ok(outOfStock);
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring stock")
    public ResponseEntity<List<Stock>> getExpiringStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "beforeDate", required = false) String beforeDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/stock/expiring - org: {}, before: {}", organizationId, beforeDate);

        LocalDate date = beforeDate != null ? LocalDate.parse(beforeDate) : LocalDate.now().plusDays(30);
        List<Stock> expiringStock = stockService.getExpiringStock(organizationId, date);
        return ResponseEntity.ok(expiringStock);
    }

    @PostMapping("/receive")
    @Operation(summary = "Receive stock into warehouse")
    public ResponseEntity<Stock> receiveStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockReceiptRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/stock/receive - product: {}, qty: {}", request.getProductId(), request.getQuantity());
        Stock stock = stockService.receiveStock(
                request.getOrganizationId(),
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getUnitCost(),
                request.getSourceType(),
                request.getSourceId(),
                request.getCreatedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    @PostMapping("/issue")
    @Operation(summary = "Issue stock from warehouse")
    public ResponseEntity<Stock> issueStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockIssueRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/stock/issue - product: {}, qty: {}", request.getProductId(), request.getQuantity());
        Stock stock = stockService.issueStock(
                request.getOrganizationId(),
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getSourceType(),
                request.getSourceId(),
                request.getCreatedBy()
        );
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/allocate")
    @Operation(summary = "Allocate stock for sales order")
    public ResponseEntity<Stock> allocateStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockAllocationRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/stock/allocate - product: {}, qty: {}", request.getProductId(), request.getQuantity());
        Stock stock = stockService.allocateStock(
                request.getOrganizationId(),
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getSalesOrderId(),
                request.getCreatedBy()
        );
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/deallocate")
    @Operation(summary = "Deallocate stock (release reservation)")
    public ResponseEntity<Stock> deallocateStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockAllocationRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/stock/deallocate - product: {}, qty: {}", request.getProductId(), request.getQuantity());
        Stock stock = stockService.deallocateStock(
                request.getOrganizationId(),
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getSalesOrderId(),
                request.getCreatedBy()
        );
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/adjust")
    @Operation(summary = "Adjust stock quantity (set absolute on-hand via newQuantity, or add/subtract via quantityDelta)")
    public ResponseEntity<Stock> adjustStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody StockAdjustmentRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        boolean hasNew = request.getNewQuantity() != null;
        boolean hasDelta = request.getQuantityDelta() != null;
        if (hasNew == hasDelta) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of newQuantity or quantityDelta");
        }
        if (hasNew && request.getNewQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newQuantity must be zero or positive");
        }
        log.info("POST /api/inventory/stock/adjust - product: {}, newQty: {}, delta: {}",
                request.getProductId(), request.getNewQuantity(), request.getQuantityDelta());
        try {
            Stock stock = stockService.adjustStock(
                    request.getOrganizationId(),
                    request.getProductId(),
                    request.getWarehouseId(),
                    request.getNewQuantity(),
                    request.getQuantityDelta(),
                    request.getReason(),
                    request.getCreatedBy()
            );
            return ResponseEntity.ok(stock);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/receive/bulk")
    @Operation(summary = "Bulk receive inventory (multiple products)")
    public ResponseEntity<Map<String, Object>> bulkReceiveStock(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody InventoryReceiveRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/stock/receive/bulk - date: {}, items: {}", request.getDate(), request.getItems().size());
        Map<String, Object> result = stockService.bulkReceiveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
