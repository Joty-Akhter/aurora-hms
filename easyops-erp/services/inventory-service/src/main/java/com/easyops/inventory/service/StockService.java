package com.easyops.inventory.service;

import com.easyops.inventory.dto.InventoryReceiveRequest;
import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.entity.StockMovement;
import com.easyops.inventory.repository.StockRepository;
import com.easyops.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
    
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    
    @Transactional(readOnly = true)
    public List<Stock> getStockByOrganization(UUID organizationId) {
        log.debug("Fetching stock for organization: {}", organizationId);
        return stockRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Stock> getStockByProduct(UUID organizationId, UUID productId) {
        log.debug("Fetching stock for product: {}", productId);
        return stockRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }
    
    @Transactional(readOnly = true)
    public List<Stock> getStockByWarehouse(UUID organizationId, UUID warehouseId) {
        log.debug("Fetching stock for warehouse: {}", warehouseId);
        return stockRepository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(UUID productId, UUID warehouseId) {
        log.debug("Getting available quantity for product: {} in warehouse: {}", productId, warehouseId);
        return stockRepository.getTotalAvailableQuantity(productId, warehouseId);
    }
    
    @Transactional(readOnly = true)
    public List<Stock> getLowStockItems(UUID organizationId) {
        log.debug("Fetching low stock items for organization: {}", organizationId);
        return stockRepository.findItemsAtReorderPoint(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Stock> getOutOfStockItems(UUID organizationId) {
        log.debug("Fetching out of stock items for organization: {}", organizationId);
        return stockRepository.findOutOfStockItems(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Stock> getExpiringStock(UUID organizationId, LocalDate beforeDate) {
        log.debug("Fetching expiring stock before: {}", beforeDate);
        return stockRepository.findExpiringStock(organizationId, beforeDate);
    }
    
    /**
     * Receive stock - increases on-hand quantity
     */
    @Transactional
    public Stock receiveStock(UUID organizationId, UUID productId, UUID warehouseId, 
                              BigDecimal quantity, BigDecimal unitCost, String sourceType, 
                              UUID sourceId, UUID createdBy) {
        log.info("Receiving stock: product={}, warehouse={}, qty={}", productId, warehouseId, quantity);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
        
        // Find or create stock record
        Stock stock = stockRepository.findMainStock(organizationId, productId, warehouseId)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setOrganizationId(organizationId);
                    newStock.setProductId(productId);
                    newStock.setWarehouseId(warehouseId);
                    newStock.setQuantityOnHand(BigDecimal.ZERO);
                    newStock.setQuantityAllocated(BigDecimal.ZERO);
                    newStock.setQuantityAvailable(BigDecimal.ZERO);
                    newStock.setUnitCost(unitCost);
                    return newStock;
                });
        
        // Update quantities
        stock.setQuantityOnHand(stock.getQuantityOnHand().add(quantity));
        stock.setQuantityAvailable(stock.getQuantityOnHand().subtract(stock.getQuantityAllocated()));
        
        // Update cost (weighted average)
        if (stock.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalValue = stock.getTotalCost().add(quantity.multiply(unitCost));
            stock.setUnitCost(totalValue.divide(stock.getQuantityOnHand(), 4, BigDecimal.ROUND_HALF_UP));
            stock.setTotalCost(stock.getQuantityOnHand().multiply(stock.getUnitCost()));
        }
        
        stock.setLastMovementAt(LocalDateTime.now());
        
        Stock savedStock = stockRepository.save(stock);
        
        // Create movement record
        createMovement(organizationId, productId, warehouseId, null, "RECEIPT", quantity,
                      unitCost, sourceType, sourceId, null, createdBy);
        
        return savedStock;
    }
    
    /**
     * Issue stock - decreases on-hand quantity (for sales, production, etc.)
     */
    @Transactional
    public Stock issueStock(UUID organizationId, UUID productId, UUID warehouseId,
                           BigDecimal quantity, String sourceType, UUID sourceId, UUID createdBy) {
        log.info("Issuing stock: product={}, warehouse={}, qty={}", productId, warehouseId, quantity);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
        
        Stock stock = stockRepository.findMainStock(organizationId, productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));
        
        if (stock.getQuantityAvailable().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient stock available. Available: " + stock.getQuantityAvailable() + ", Requested: " + quantity);
        }
        
        // Update quantities
        stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(quantity));
        stock.setQuantityAvailable(stock.getQuantityOnHand().subtract(stock.getQuantityAllocated()));
        stock.setTotalCost(stock.getQuantityOnHand().multiply(stock.getUnitCost()));
        stock.setLastMovementAt(LocalDateTime.now());
        
        Stock savedStock = stockRepository.save(stock);
        
        // Create movement record
        createMovement(organizationId, productId, warehouseId, null, "ISSUE", quantity.negate(),
                      stock.getUnitCost(), sourceType, sourceId, null, createdBy);
        
        return savedStock;
    }
    
    /**
     * Allocate stock - reserves stock for sales orders
     */
    @Transactional
    public Stock allocateStock(UUID organizationId, UUID productId, UUID warehouseId,
                              BigDecimal quantity, UUID salesOrderId, UUID createdBy) {
        log.info("Allocating stock: product={}, warehouse={}, qty={}", productId, warehouseId, quantity);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
        
        Stock stock = stockRepository.findMainStock(organizationId, productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));
        
        if (stock.getQuantityAvailable().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient stock available for allocation. Available: " + stock.getQuantityAvailable() + ", Requested: " + quantity);
        }
        
        // Allocate stock
        stock.setQuantityAllocated(stock.getQuantityAllocated().add(quantity));
        stock.setQuantityAvailable(stock.getQuantityOnHand().subtract(stock.getQuantityAllocated()));
        stock.setLastMovementAt(LocalDateTime.now());
        
        return stockRepository.save(stock);
    }
    
    /**
     * Deallocate stock - releases reserved stock
     */
    @Transactional
    public Stock deallocateStock(UUID organizationId, UUID productId, UUID warehouseId,
                                BigDecimal quantity, UUID salesOrderId, UUID createdBy) {
        log.info("Deallocating stock: product={}, warehouse={}, qty={}", productId, warehouseId, quantity);
        
        Stock stock = stockRepository.findMainStock(organizationId, productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));
        
        stock.setQuantityAllocated(stock.getQuantityAllocated().subtract(quantity));
        stock.setQuantityAvailable(stock.getQuantityOnHand().subtract(stock.getQuantityAllocated()));
        stock.setLastMovementAt(LocalDateTime.now());
        
        return stockRepository.save(stock);
    }
    
    /**
     * Adjust stock - manual stock adjustment (increase or decrease).
     * Pass {@code newQuantity} for an absolute target on-hand, or {@code quantityDelta} to add/subtract from current on-hand.
     * Exactly one of the two must be non-null.
     */
    @Transactional
    public Stock adjustStock(UUID organizationId, UUID productId, UUID warehouseId,
                            BigDecimal newQuantity, BigDecimal quantityDelta, String reason, UUID createdBy) {
        boolean hasNew = newQuantity != null;
        boolean hasDelta = quantityDelta != null;
        if (hasNew == hasDelta) {
            throw new IllegalArgumentException("Provide exactly one of newQuantity or quantityDelta");
        }
        
        Stock stock = findOrCreateMainStockForAdjustment(organizationId, productId, warehouseId, newQuantity, quantityDelta);
        
        BigDecimal oldQuantity = stock.getQuantityOnHand();
        BigDecimal resolvedOnHand = hasDelta ? oldQuantity.add(quantityDelta) : newQuantity;
        if (resolvedOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting quantity cannot be negative");
        }
        
        BigDecimal adjustmentQty = resolvedOnHand.subtract(oldQuantity);
        
        log.info("Adjusting stock: product={}, warehouse={}, resolved on-hand={}, delta={}",
                productId, warehouseId, resolvedOnHand, adjustmentQty);
        
        stock.setQuantityOnHand(resolvedOnHand);
        stock.setQuantityAvailable(stock.getQuantityOnHand().subtract(stock.getQuantityAllocated()));
        stock.setTotalCost(stock.getQuantityOnHand().multiply(stock.getUnitCost()));
        stock.setLastMovementAt(LocalDateTime.now());
        
        Stock savedStock = stockRepository.save(stock);
        
        // Create movement record
        createMovement(organizationId, productId, warehouseId, null, "ADJUSTMENT", adjustmentQty,
                      stock.getUnitCost(), "MANUAL", null, reason, createdBy);
        
        return savedStock;
    }
    
    /**
     * Main stock row (no batch/serial) may not exist until the first receipt or positive adjustment.
     * Mirrors {@link #receiveStock} find-or-create behavior.
     */
    private Stock findOrCreateMainStockForAdjustment(UUID organizationId, UUID productId, UUID warehouseId,
                                                     BigDecimal newQuantity, BigDecimal quantityDelta) {
        return stockRepository.findMainStock(organizationId, productId, warehouseId)
                .orElseGet(() -> {
                    boolean hasDelta = quantityDelta != null;
                    if (hasDelta && quantityDelta.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                "No stock record exists for this product in this warehouse. "
                                        + "Receive inventory first, or use a positive quantity change to add stock.");
                    }
                    if (!hasDelta && newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                "No stock record exists for this product in this warehouse. "
                                        + "Receive inventory or use a positive adjustment before setting quantity to zero.");
                    }
                    Stock s = new Stock();
                    s.setOrganizationId(organizationId);
                    s.setProductId(productId);
                    s.setWarehouseId(warehouseId);
                    s.setQuantityOnHand(BigDecimal.ZERO);
                    s.setQuantityAllocated(BigDecimal.ZERO);
                    s.setQuantityAvailable(BigDecimal.ZERO);
                    s.setQuantityOnOrder(BigDecimal.ZERO);
                    s.setQuantityInTransit(BigDecimal.ZERO);
                    s.setUnitCost(BigDecimal.ZERO);
                    s.setTotalCost(BigDecimal.ZERO);
                    return s;
                });
    }
    
    /**
     * Helper method to create stock movement records
     */
    private void createMovement(UUID organizationId, UUID productId, UUID warehouseId,
                               UUID locationId, String transactionType, BigDecimal quantity,
                               BigDecimal unitCost, String sourceType, UUID sourceId,
                               String reason, UUID createdBy) {
        
        StockMovement movement = new StockMovement();
        movement.setOrganizationId(organizationId);
        movement.setTransactionNumber(generateTransactionNumber(transactionType));
        movement.setTransactionDate(LocalDate.now());
        movement.setTransactionType(transactionType);
        movement.setProductId(productId);
        movement.setWarehouseId(warehouseId);
        movement.setLocationId(locationId);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setTotalCost(quantity.multiply(unitCost));
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setAdjustmentReason(reason);
        movement.setStatus("COMPLETED");
        movement.setCreatedBy(createdBy);
        
        movementRepository.save(movement);
        log.debug("Created stock movement: {} for product: {}", transactionType, productId);
    }
    
    /**
     * Bulk receive stock - receive multiple products at once
     */
    @Transactional
    public Map<String, Object> bulkReceiveStock(InventoryReceiveRequest request) {
        log.info("Bulk receiving stock: date={}, warehouse={}, items={}", 
                request.getDate(), request.getWarehouseId(), request.getItems().size());
        
        List<Stock> receivedStocks = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        
        for (InventoryReceiveRequest.ReceiveItem item : request.getItems()) {
            try {
                // Use quantity directly
                BigDecimal quantity = item.getQuantity();
                
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Invalid quantity for product: " + item.getProductId());
                    continue;
                }
                
                // Use Trade Price as unit cost
                BigDecimal unitCost = item.getTradePrice();
                if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                    unitCost = BigDecimal.ZERO;
                }
                
                Stock stock = receiveStock(
                        request.getOrganizationId(),
                        item.getProductId(),
                        request.getWarehouseId(),
                        quantity,
                        unitCost,
                        "MANUAL",
                        null,
                        request.getCreatedBy()
                );
                
                receivedStocks.add(stock);
                totalQuantity = totalQuantity.add(quantity);
                totalAmount = totalAmount.add(quantity.multiply(unitCost));
                
            } catch (Exception e) {
                log.error("Error receiving stock for product: {}", item.getProductId(), e);
                errors.add("Product " + item.getProductId() + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", errors.isEmpty());
        result.put("receivedCount", receivedStocks.size());
        result.put("totalQuantity", totalQuantity);
        result.put("totalAmount", totalAmount);
        result.put("stocks", receivedStocks);
        
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        
        return result;
    }
    
    private String generateTransactionNumber(String type) {
        return type.substring(0, 3).toUpperCase() + "-" + System.currentTimeMillis();
    }
}

