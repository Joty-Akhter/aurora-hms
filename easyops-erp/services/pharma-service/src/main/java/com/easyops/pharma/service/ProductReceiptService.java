package com.easyops.pharma.service;

import com.easyops.pharma.client.InventoryClient;
import com.easyops.pharma.entity.ProductReceipt;
import com.easyops.pharma.entity.ProductReceiptLine;
import com.easyops.pharma.repository.ProductReceiptRepository;
import com.easyops.pharma.repository.ProductReceiptLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReceiptService {
    
    private final ProductReceiptRepository receiptRepository;
    private final ProductReceiptLineRepository receiptLineRepository;
    private final InventoryClient inventoryClient;
    
    @Transactional(readOnly = true)
    public List<ProductReceipt> getAllReceipts(UUID organizationId) {
        log.debug("Fetching all product receipts for organization: {}", organizationId);
        return receiptRepository.findAllByOrganizationOrderByDate(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ProductReceipt> getReceiptsByDateRange(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching receipts for date range: {} to {}", startDate, endDate);
        return receiptRepository.findByOrganizationIdAndReceiptDateBetween(organizationId, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public ProductReceipt getReceiptById(UUID id) {
        log.debug("Fetching receipt by ID: {}", id);
        ProductReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product receipt not found with ID: " + id));
        
        // Load receipt lines
        receipt.setReceiptLines(receiptLineRepository.findByProductReceiptId(id));
        
        return receipt;
    }
    
    @Transactional
    @CacheEvict(value = "productReceipts", allEntries = true)
    public ProductReceipt createReceipt(ProductReceipt receipt) {
        log.info("Creating new product receipt");
        
        // Generate receipt number if not provided
        if (receipt.getReceiptNumber() == null || receipt.getReceiptNumber().isEmpty()) {
            receipt.setReceiptNumber(generateReceiptNumber(receipt.getOrganizationId()));
        }
        
        // Validate receipt number uniqueness
        if (receiptRepository.findByReceiptNumber(receipt.getReceiptNumber()).isPresent()) {
            throw new RuntimeException("Receipt number already exists: " + receipt.getReceiptNumber());
        }
        
        // Calculate total value from lines
        if (receipt.getReceiptLines() != null && !receipt.getReceiptLines().isEmpty()) {
            BigDecimal totalValue = BigDecimal.ZERO;
            for (ProductReceiptLine line : receipt.getReceiptLines()) {
                // Calculate line amount: Quantity × TP with VAT
                if (line.getQuantity() != null && line.getTpWithVat() != null) {
                    line.setAmount(line.getQuantity().multiply(line.getTpWithVat()));
                    totalValue = totalValue.add(line.getAmount());
                }
            }
            receipt.setTotalValue(totalValue);
        }
        
        // Set default status
        if (receipt.getStatus() == null) {
            receipt.setStatus("DRAFT");
        }
        
        // Save receipt first
        ProductReceipt savedReceipt = receiptRepository.save(receipt);
        
        // Save receipt lines
        if (receipt.getReceiptLines() != null) {
            for (ProductReceiptLine line : receipt.getReceiptLines()) {
                line.setProductReceiptId(savedReceipt.getId());
                receiptLineRepository.save(line);
            }
        }
        
        return savedReceipt;
    }
    
    @Transactional
    @CacheEvict(value = "productReceipts", allEntries = true)
    public ProductReceipt updateReceipt(UUID id, ProductReceipt receipt) {
        log.info("Updating product receipt: {}", id);
        ProductReceipt existing = getReceiptById(id);
        
        // Don't allow updating submitted/completed receipts
        if ("SUBMITTED".equals(existing.getStatus()) || "COMPLETED".equals(existing.getStatus())) {
            throw new RuntimeException("Cannot update receipt with status: " + existing.getStatus());
        }
        
        existing.setReceiptDate(receipt.getReceiptDate());
        existing.setNotes(receipt.getNotes());
        existing.setUserName(receipt.getUserName());
        existing.setUserDesignation(receipt.getUserDesignation());
        existing.setUpdatedBy(receipt.getUpdatedBy());
        
        // Update receipt lines
        if (receipt.getReceiptLines() != null) {
            // Delete existing lines
            receiptLineRepository.findByProductReceiptId(id).forEach(receiptLineRepository::delete);
            
            // Add new lines
            BigDecimal totalValue = BigDecimal.ZERO;
            for (ProductReceiptLine line : receipt.getReceiptLines()) {
                line.setProductReceiptId(id);
                if (line.getQuantity() != null && line.getTpWithVat() != null) {
                    line.setAmount(line.getQuantity().multiply(line.getTpWithVat()));
                    totalValue = totalValue.add(line.getAmount());
                }
                receiptLineRepository.save(line);
            }
            existing.setTotalValue(totalValue);
        }
        
        return receiptRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = "productReceipts", allEntries = true)
    public ProductReceipt submitReceipt(UUID id) {
        log.info("Submitting product receipt: {}", id);
        ProductReceipt receipt = getReceiptById(id);
        
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be submitted");
        }
        
        if (receipt.getReceiptLines() == null || receipt.getReceiptLines().isEmpty()) {
            throw new RuntimeException("Cannot submit receipt without lines");
        }
        
        // Get or find central depot warehouse
        UUID warehouseId = getOrCreateCentralDepotWarehouse(receipt.getOrganizationId());
        
        // Update inventory stock for each receipt line
        for (ProductReceiptLine line : receipt.getReceiptLines()) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Skipping line with invalid quantity: {}", line.getId());
                continue;
            }
            
            try {
                // Call inventory service to receive stock
                Map<String, Object> stockRequest = new HashMap<>();
                stockRequest.put("organizationId", receipt.getOrganizationId().toString());
                stockRequest.put("productId", line.getProductId().toString());
                stockRequest.put("warehouseId", warehouseId.toString());
                stockRequest.put("quantity", line.getQuantity());
                stockRequest.put("unitCost", line.getTpWithVat() != null ? line.getTpWithVat() : BigDecimal.ZERO);
                stockRequest.put("sourceType", "PRODUCT_RECEIPT");
                stockRequest.put("sourceId", receipt.getId().toString());
                stockRequest.put("createdBy", receipt.getCreatedBy() != null ? receipt.getCreatedBy().toString() : null);
                
                Map<String, Object> stockResponse = inventoryClient.receiveStock(stockRequest);
                log.info("Successfully updated inventory stock for product {}: quantity {}", 
                        line.getProductId(), line.getQuantity());
                        
            } catch (Exception e) {
                log.error("Failed to update inventory stock for product {}: {}", 
                        line.getProductId(), e.getMessage());
                throw new RuntimeException("Failed to update inventory stock: " + e.getMessage(), e);
            }
        }
        
        receipt.setStatus("SUBMITTED");
        return receiptRepository.save(receipt);
    }
    
    /**
     * Get or create central depot warehouse
     * Central depot is the main warehouse where products are received from factory
     */
    private UUID getOrCreateCentralDepotWarehouse(UUID organizationId) {
        try {
            // Get all warehouses for the organization
            List<Map<String, Object>> warehouses = inventoryClient.getWarehouses(organizationId);
            
            // Find central depot warehouse (type MAIN or name contains "CENTRAL" or "DEPOT")
            for (Map<String, Object> warehouse : warehouses) {
                String warehouseType = (String) warehouse.get("warehouseType");
                String warehouseName = (String) warehouse.get("name");
                
                if ("MAIN".equals(warehouseType) || 
                    (warehouseName != null && (warehouseName.toUpperCase().contains("CENTRAL") || 
                                               warehouseName.toUpperCase().contains("DEPOT")))) {
                    UUID warehouseId = UUID.fromString(warehouse.get("id").toString());
                    log.info("Found central depot warehouse: {} ({})", warehouseName, warehouseId);
                    return warehouseId;
                }
            }
            
            // If not found, create central depot warehouse
            log.info("Central depot warehouse not found, creating one...");
            Map<String, Object> newWarehouse = new HashMap<>();
            newWarehouse.put("organizationId", organizationId.toString());
            newWarehouse.put("code", "CENTRAL-DEPOT");
            newWarehouse.put("name", "Central Depot");
            newWarehouse.put("warehouseType", "MAIN");
            newWarehouse.put("isActive", true);
            newWarehouse.put("status", "ACTIVE");
            
            Map<String, Object> createdWarehouse = inventoryClient.createWarehouse(newWarehouse);
            UUID warehouseId = UUID.fromString(createdWarehouse.get("id").toString());
            log.info("Created central depot warehouse: {}", warehouseId);
            return warehouseId;
            
        } catch (Exception e) {
            log.error("Failed to get or create central depot warehouse: {}", e.getMessage());
            throw new RuntimeException("Failed to get or create central depot warehouse: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    @CacheEvict(value = "productReceipts", allEntries = true)
    public void deleteReceipt(UUID id) {
        log.info("Deleting product receipt: {}", id);
        ProductReceipt receipt = getReceiptById(id);
        
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be deleted");
        }
        
        // Delete receipt lines first
        receiptLineRepository.findByProductReceiptId(id).forEach(receiptLineRepository::delete);
        
        receiptRepository.delete(receipt);
    }
    
    private String generateReceiptNumber(UUID organizationId) {
        String prefix = "PR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orgPrefix = organizationId.toString().substring(0, 8).toUpperCase();
        return String.format("%s-%s-%s-%d", prefix, dateStr, orgPrefix, System.currentTimeMillis() % 10000);
    }
}

