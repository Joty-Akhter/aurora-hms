package com.easyops.inventory.service;

import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.entity.StockMovement;
import com.easyops.inventory.entity.Product;
import com.easyops.inventory.repository.StockRepository;
import com.easyops.inventory.repository.StockMovementRepository;
import com.easyops.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryValuationService {
    
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    
    /**
     * Calculate COGS using FIFO method
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCOGS_FIFO(UUID productId, UUID warehouseId, BigDecimal quantity, LocalDate asOfDate) {
        log.debug("Calculating FIFO COGS for product: {}, quantity: {}", productId, quantity);
        
        List<StockMovement> receipts = movementRepository.findByDateRange(
            null, // Will filter by product
            LocalDate.now().minusYears(5), // Look back 5 years
            asOfDate
        ).stream()
            .filter(m -> m.getProductId().equals(productId) && m.getWarehouseId().equals(warehouseId))
            .filter(m -> "RECEIPT".equals(m.getTransactionType()))
            .sorted(Comparator.comparing(StockMovement::getTransactionDate))
            .toList();
        
        BigDecimal remainingQty = quantity;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (StockMovement receipt : receipts) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal receiptQty = receipt.getQuantity();
            BigDecimal qtyToUse = remainingQty.min(receiptQty);
            
            totalCost = totalCost.add(qtyToUse.multiply(receipt.getUnitCost()));
            remainingQty = remainingQty.subtract(qtyToUse);
        }
        
        return totalCost.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate COGS using LIFO method
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCOGS_LIFO(UUID productId, UUID warehouseId, BigDecimal quantity, LocalDate asOfDate) {
        log.debug("Calculating LIFO COGS for product: {}, quantity: {}", productId, quantity);
        
        List<StockMovement> receipts = movementRepository.findByDateRange(
            null,
            LocalDate.now().minusYears(5),
            asOfDate
        ).stream()
            .filter(m -> m.getProductId().equals(productId) && m.getWarehouseId().equals(warehouseId))
            .filter(m -> "RECEIPT".equals(m.getTransactionType()))
            .sorted(Comparator.comparing(StockMovement::getTransactionDate).reversed()) // Latest first
            .toList();
        
        BigDecimal remainingQty = quantity;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (StockMovement receipt : receipts) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal receiptQty = receipt.getQuantity();
            BigDecimal qtyToUse = remainingQty.min(receiptQty);
            
            totalCost = totalCost.add(qtyToUse.multiply(receipt.getUnitCost()));
            remainingQty = remainingQty.subtract(qtyToUse);
        }
        
        return totalCost.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate COGS using Weighted Average method
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCOGS_WeightedAverage(UUID productId, UUID warehouseId, BigDecimal quantity) {
        log.debug("Calculating Weighted Average COGS for product: {}, quantity: {}", productId, quantity);
        
        Stock stock = stockRepository.findMainStock(null, productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));
        
        BigDecimal unitCost = stock.getUnitCost();
        BigDecimal totalCost = quantity.multiply(unitCost);
        
        return totalCost.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total inventory value for an organization using TP (Trade Price) * quantity
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateInventoryValue(UUID organizationId) {
        log.debug("Calculating total inventory value for organization: {} using TP * quantity", organizationId);
        
        List<Stock> allStock = stockRepository.findByOrganizationId(organizationId);
        
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        
        for (Stock stock : allStock) {
            // Get product to get TP (wholesalePrice)
            Optional<Product> productOpt = productRepository.findById(stock.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                BigDecimal tp = product.getWholesalePrice();
                if (tp != null && tp.compareTo(BigDecimal.ZERO) > 0) {
                    // Calculate value as TP * quantity on hand
                    BigDecimal itemValue = tp.multiply(stock.getQuantityOnHand());
                    totalValue = totalValue.add(itemValue);
                }
            }
            totalQuantity = totalQuantity.add(stock.getQuantityOnHand());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalValue", totalValue);
        result.put("totalQuantity", totalQuantity);
        result.put("itemCount", allStock.size());
        result.put("calculatedAt", LocalDate.now());
        
        return result;
    }
    
    /**
     * Get inventory value by warehouse using TP (Trade Price) * quantity
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInventoryValueByWarehouse(UUID organizationId) {
        log.debug("Calculating inventory value by warehouse for organization: {} using TP * quantity", organizationId);
        
        List<Stock> allStock = stockRepository.findByOrganizationId(organizationId);
        
        Map<UUID, BigDecimal> warehouseValues = new HashMap<>();
        Map<UUID, BigDecimal> warehouseQuantities = new HashMap<>();
        
        for (Stock stock : allStock) {
            UUID whId = stock.getWarehouseId();
            
            // Get product to get TP (wholesalePrice)
            Optional<Product> productOpt = productRepository.findById(stock.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                BigDecimal tp = product.getWholesalePrice();
                if (tp != null && tp.compareTo(BigDecimal.ZERO) > 0) {
                    // Calculate value as TP * quantity on hand
                    BigDecimal itemValue = tp.multiply(stock.getQuantityOnHand());
                    warehouseValues.merge(whId, itemValue, BigDecimal::add);
                }
            }
            warehouseQuantities.merge(whId, stock.getQuantityOnHand(), BigDecimal::add);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (UUID whId : warehouseValues.keySet()) {
            Map<String, Object> whData = new HashMap<>();
            whData.put("warehouseId", whId);
            whData.put("totalValue", warehouseValues.get(whId));
            whData.put("totalQuantity", warehouseQuantities.get(whId));
            result.add(whData);
        }
        
        return result;
    }
}

