package com.easyops.inventory.controller;

import com.easyops.inventory.dto.PurchaseReceiptRequest;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.PurchaseReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/purchase-receipts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Purchase Integration", description = "Purchase order goods receipt APIs")
@CrossOrigin(origins = "*")
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;
    private final InventoryRbacService inventoryRbac;

    @PostMapping
    @Operation(summary = "Process goods receipt from purchase order")
    public ResponseEntity<Map<String, Object>> processPurchaseReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody PurchaseReceiptRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, request.getOrganizationId());
        log.info("POST /api/inventory/purchase-receipts - PO: {}", request.getPurchaseOrderId());
        Map<String, Object> result = purchaseReceiptService.processPurchaseReceipt(request);
        return ResponseEntity.ok(result);
    }
}
