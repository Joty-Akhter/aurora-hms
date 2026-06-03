package com.easyops.inventory.controller;

import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.InventoryAccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/accounting")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting Integration", description = "GL and COGS integration APIs")
@CrossOrigin(origins = "*")
public class AccountingIntegrationController {

    private final InventoryAccountingService accountingService;
    private final InventoryRbacService inventoryRbac;

    @PostMapping("/cogs")
    @Operation(summary = "Generate COGS journal entry for stock issue")
    public ResponseEntity<Map<String, Object>> generateCOGSEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID organizationId = UUID.fromString(request.get("organizationId").toString());
        inventoryRbac.requireInventoryManage(actor, organizationId);
        log.info("POST /api/inventory/accounting/cogs");

        UUID productId = UUID.fromString(request.get("productId").toString());
        UUID warehouseId = UUID.fromString(request.get("warehouseId").toString());
        BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
        String valuationMethod = request.getOrDefault("valuationMethod", "WEIGHTED_AVERAGE").toString();
        LocalDate transactionDate = request.containsKey("transactionDate")
                ? LocalDate.parse(request.get("transactionDate").toString())
                : LocalDate.now();
        UUID salesOrderId = request.containsKey("salesOrderId")
                ? UUID.fromString(request.get("salesOrderId").toString())
                : null;

        Map<String, Object> journalEntry = accountingService.createCOGSJournalEntry(
                organizationId, productId, warehouseId, quantity, valuationMethod, transactionDate, salesOrderId, actor);

        return ResponseEntity.ok(journalEntry);
    }

    @PostMapping("/receipt")
    @Operation(summary = "Generate GL entry for stock receipt")
    public ResponseEntity<Map<String, Object>> generateReceiptEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID organizationId = UUID.fromString(request.get("organizationId").toString());
        inventoryRbac.requireInventoryManage(actor, organizationId);
        log.info("POST /api/inventory/accounting/receipt");

        UUID productId = UUID.fromString(request.get("productId").toString());
        UUID warehouseId = UUID.fromString(request.get("warehouseId").toString());
        BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
        BigDecimal unitCost = new BigDecimal(request.get("unitCost").toString());
        LocalDate transactionDate = request.containsKey("transactionDate")
                ? LocalDate.parse(request.get("transactionDate").toString())
                : LocalDate.now();

        Map<String, Object> journalEntry = accountingService.createReceiptJournalEntry(
                organizationId, productId, warehouseId, quantity, unitCost, transactionDate, actor);

        return ResponseEntity.ok(journalEntry);
    }

    @PostMapping("/adjustment")
    @Operation(summary = "Generate GL entry for inventory adjustment")
    public ResponseEntity<Map<String, Object>> generateAdjustmentEntry(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID organizationId = UUID.fromString(request.get("organizationId").toString());
        inventoryRbac.requireInventoryManage(actor, organizationId);
        log.info("POST /api/inventory/accounting/adjustment");

        UUID productId = UUID.fromString(request.get("productId").toString());
        BigDecimal adjustmentQuantity = new BigDecimal(request.get("adjustmentQuantity").toString());
        BigDecimal unitCost = new BigDecimal(request.get("unitCost").toString());
        String reason = request.getOrDefault("reason", "Inventory adjustment").toString();
        LocalDate transactionDate = request.containsKey("transactionDate")
                ? LocalDate.parse(request.get("transactionDate").toString())
                : LocalDate.now();

        Map<String, Object> journalEntry = accountingService.createAdjustmentJournalEntry(
                organizationId, productId, adjustmentQuantity, unitCost, reason, transactionDate, actor);

        return ResponseEntity.ok(journalEntry);
    }
}
