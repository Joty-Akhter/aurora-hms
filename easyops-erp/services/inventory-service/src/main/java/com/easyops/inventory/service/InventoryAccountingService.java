package com.easyops.inventory.service;

import com.easyops.inventory.client.AccountingJournalClient;
import com.easyops.inventory.entity.Product;
import com.easyops.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAccountingService {

    static final String DEFAULT_INVENTORY = "1143";
    static final String DEFAULT_COGS = "5010";
    static final String DEFAULT_AP = "2110";
    static final String DEFAULT_ADJ_INCOME = "4040";
    static final String DEFAULT_ADJ_EXPENSE = "5210";

    private final ProductRepository productRepository;
    private final InventoryValuationService valuationService;
    private final AccountingJournalClient accountingJournalClient;

    @Transactional
    public Map<String, Object> createReceiptJournalEntry(
            UUID organizationId, UUID productId, UUID warehouseId,
            BigDecimal quantity, BigDecimal unitCost, LocalDate transactionDate,
            UUID actorUserId) {

        log.info("Creating GL entry for stock receipt - product: {}, qty: {}, cost: {}",
                productId, quantity, unitCost);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal totalCost = quantity.multiply(unitCost);
        String inventoryCode = resolveInventoryCode(product, actorUserId);

        List<Map<String, Object>> lines = List.of(
                journalLine(inventoryCode, totalCost, BigDecimal.ZERO, "Inventory receipt - " + product.getName()),
                journalLine(DEFAULT_AP, BigDecimal.ZERO, totalCost, "Inventory receipt - " + product.getName())
        );

        Map<String, Object> posted = postJournal(
                organizationId, transactionDate, "INVENTORY_RECEIPT",
                "Stock receipt for product: " + product.getName(),
                productId.toString(), lines, actorUserId);

        log.info("Posted GL entry for stock receipt. Amount: {}", totalCost);
        return posted;
    }

    @Transactional
    public Map<String, Object> createCOGSJournalEntry(
            UUID organizationId, UUID productId, UUID warehouseId,
            BigDecimal quantity, String valuationMethod, LocalDate transactionDate,
            UUID salesOrderId, UUID actorUserId) {

        log.info("Creating COGS GL entry - product: {}, qty: {}, method: {}",
                productId, quantity, valuationMethod);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal cogsAmount = calculateCogs(productId, warehouseId, quantity, valuationMethod, transactionDate);
        String cogsCode = resolveCogsCode(product, actorUserId);
        String inventoryCode = resolveInventoryCode(product, actorUserId);

        List<Map<String, Object>> lines = List.of(
                journalLine(cogsCode, cogsAmount, BigDecimal.ZERO, "COGS - " + product.getName()),
                journalLine(inventoryCode, BigDecimal.ZERO, cogsAmount, "Inventory reduction - " + product.getName())
        );

        String referenceId = salesOrderId != null ? salesOrderId.toString() : productId.toString();
        Map<String, Object> posted = postJournal(
                organizationId, transactionDate, "COGS",
                "COGS for sale of product: " + product.getName(),
                referenceId, lines, actorUserId);

        log.info("Posted COGS GL entry. Amount: {}", cogsAmount);
        return posted;
    }

    @Transactional
    public Map<String, Object> createAdjustmentJournalEntry(
            UUID organizationId, UUID productId, BigDecimal adjustmentQuantity,
            BigDecimal unitCost, String reason, LocalDate transactionDate,
            UUID actorUserId) {

        log.info("Creating GL entry for inventory adjustment - product: {}, qty: {}",
                productId, adjustmentQuantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal adjustmentValue = adjustmentQuantity.multiply(unitCost).abs();
        String inventoryCode = resolveInventoryCode(product, actorUserId);

        List<Map<String, Object>> lines = new ArrayList<>();
        if (adjustmentQuantity.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(journalLine(inventoryCode, adjustmentValue, BigDecimal.ZERO, "Inventory adjustment - " + reason));
            lines.add(journalLine(DEFAULT_ADJ_INCOME, BigDecimal.ZERO, adjustmentValue, "Adjustment income"));
        } else {
            lines.add(journalLine(DEFAULT_ADJ_EXPENSE, adjustmentValue, BigDecimal.ZERO, "Adjustment expense - " + reason));
            lines.add(journalLine(inventoryCode, BigDecimal.ZERO, adjustmentValue, "Inventory reduction"));
        }

        Map<String, Object> posted = postJournal(
                organizationId, transactionDate, "INVENTORY_ADJUSTMENT",
                "Inventory adjustment - " + reason,
                productId.toString(), lines, actorUserId);

        log.info("Posted adjustment GL entry. Amount: {}", adjustmentValue);
        return posted;
    }

    private BigDecimal calculateCogs(UUID productId, UUID warehouseId, BigDecimal quantity,
                                    String valuationMethod, LocalDate transactionDate) {
        return switch (valuationMethod != null ? valuationMethod.toUpperCase() : "WEIGHTED_AVERAGE") {
            case "FIFO" -> valuationService.calculateCOGS_FIFO(productId, warehouseId, quantity, transactionDate);
            case "LIFO" -> valuationService.calculateCOGS_LIFO(productId, warehouseId, quantity, transactionDate);
            default -> valuationService.calculateCOGS_WeightedAverage(productId, warehouseId, quantity);
        };
    }

    private String resolveInventoryCode(Product product, UUID actorUserId) {
        String code = accountingJournalClient.resolveAccountCode(product.getInventoryGlAccountId(), actorUserId);
        return code != null ? code : DEFAULT_INVENTORY;
    }

    private String resolveCogsCode(Product product, UUID actorUserId) {
        String code = accountingJournalClient.resolveAccountCode(product.getCogsGlAccountId(), actorUserId);
        return code != null ? code : DEFAULT_COGS;
    }

    private Map<String, Object> postJournal(UUID organizationId, LocalDate entryDate, String journalType,
                                            String description, String referenceId,
                                            List<Map<String, Object>> lines, UUID actorUserId) {
        Map<String, Object> journalEntry = new HashMap<>();
        journalEntry.put("organizationId", organizationId.toString());
        journalEntry.put("entryDate", entryDate.toString());
        journalEntry.put("journalType", journalType);
        journalEntry.put("description", description);
        journalEntry.put("referenceId", referenceId);
        journalEntry.put("status", "POSTED");
        journalEntry.put("lines", lines);
        return accountingJournalClient.createAndPostJournal(journalEntry, actorUserId);
    }

    private static Map<String, Object> journalLine(String accountCode, BigDecimal debit, BigDecimal credit, String desc) {
        Map<String, Object> line = new HashMap<>();
        line.put("accountCode", accountCode);
        line.put("debitAmount", debit);
        line.put("creditAmount", credit);
        line.put("description", desc);
        return line;
    }
}
