package com.easyops.inventory.service;

import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.entity.StockCount;
import com.easyops.inventory.entity.StockCountLine;
import com.easyops.inventory.repository.StockCountRepository;
import com.easyops.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCountService {
    
    private final StockCountRepository stockCountRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    
    @Transactional(readOnly = true)
    public List<StockCount> getAllCounts(UUID organizationId) {
        log.debug("Fetching all stock counts for organization: {}", organizationId);
        return stockCountRepository.findByOrganizationIdOrderByCountDateDesc(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<StockCount> getCountsByStatus(UUID organizationId, String status) {
        log.debug("Fetching stock counts with status: {} for organization: {}", status, organizationId);
        return stockCountRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<StockCount> getCountsByWarehouse(UUID organizationId, UUID warehouseId) {
        log.debug("Fetching stock counts for warehouse: {}", warehouseId);
        return stockCountRepository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId);
    }
    
    @Transactional(readOnly = true)
    public StockCount getCountById(UUID id) {
        log.debug("Fetching stock count by ID: {}", id);
        return stockCountRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Stock count not found with ID: " + id));
    }
    
    @Transactional
    public StockCount createCount(StockCount count) {
        log.info("Creating stock count: {} for warehouse: {}", count.getCountNumber(), count.getWarehouseId());
        
        if (stockCountRepository.existsByCountNumber(count.getCountNumber())) {
            throw new RuntimeException("Count number already exists: " + count.getCountNumber());
        }
        
        count.setStatus("SCHEDULED");
        count.setCountDate(LocalDate.now());
        
        return stockCountRepository.save(count);
    }
    
    @Transactional
    public StockCount startCount(UUID id, UUID userId) {
        log.info("Starting stock count: {} by user: {}", id, userId);
        
        StockCount count = getCountById(id);
        
        if (!"SCHEDULED".equals(count.getStatus())) {
            throw new RuntimeException("Can only start scheduled counts");
        }
        
        // Generate count lines from current stock
        List<Stock> warehouseStock = stockRepository.findByOrganizationIdAndWarehouseId(
            count.getOrganizationId(), count.getWarehouseId());
        
        int lineNumber = 1;
        for (Stock stock : warehouseStock) {
            StockCountLine line = new StockCountLine();
            line.setLineNumber(lineNumber++);
            line.setProductId(stock.getProductId());
            line.setLocationId(stock.getLocationId());
            line.setBatchNumber(stock.getBatchNumber());
            line.setSystemQuantity(stock.getQuantityOnHand());
            line.setUnitCost(stock.getUnitCost());
            line.setCountedBy(userId);
            
            count.addLine(line);
        }
        
        count.setStatus("IN_PROGRESS");
        count.setStartedAt(LocalDateTime.now());
        count.setCountedBy(userId);
        
        return stockCountRepository.save(count);
    }
    
    @Transactional
    public StockCount recordCount(UUID countId, UUID productId, BigDecimal countedQuantity, UUID userId) {
        log.info("Recording count for product: {} in count: {}", productId, countId);
        
        StockCount count = getCountById(countId);
        
        if (!"IN_PROGRESS".equals(count.getStatus())) {
            throw new RuntimeException("Count is not in progress");
        }
        
        // Find and update the line
        StockCountLine line = count.getLines().stream()
            .filter(l -> l.getProductId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Product not found in count"));
        
        line.setCountedQuantity(countedQuantity);
        line.setCountedAt(LocalDateTime.now());
        line.setCountedBy(userId);
        
        // Variance is calculated by trigger
        
        return stockCountRepository.save(count);
    }
    
    @Transactional
    public StockCount completeCount(UUID id) {
        log.info("Completing stock count: {}", id);
        
        StockCount count = getCountById(id);
        
        if (!"IN_PROGRESS".equals(count.getStatus())) {
            throw new RuntimeException("Count is not in progress");
        }
        
        // Calculate total variance
        BigDecimal totalVariance = count.getLines().stream()
            .map(StockCountLine::getVarianceValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        count.setTotalVarianceValue(totalVariance);
        count.setStatus("COMPLETED");
        count.setCompletedAt(LocalDateTime.now());
        
        return stockCountRepository.save(count);
    }
    
    @Transactional
    public StockCount approveCount(UUID id, UUID approvedBy) {
        log.info("Approving stock count: {} by user: {}", id, approvedBy);
        
        StockCount count = getCountById(id);
        
        if (!"COMPLETED".equals(count.getStatus())) {
            throw new RuntimeException("Can only approve completed counts");
        }
        
        // Apply adjustments to stock
        for (StockCountLine line : count.getLines()) {
            if (line.getVarianceQuantity().compareTo(BigDecimal.ZERO) != 0) {
                try {
                    stockService.adjustStock(
                        count.getOrganizationId(),
                        line.getProductId(),
                        count.getWarehouseId(),
                        line.getCountedQuantity(),
                        null,
                        "Physical count adjustment - " + count.getCountNumber(),
                        approvedBy
                    );
                } catch (Exception e) {
                    log.error("Failed to adjust stock for product: {}", line.getProductId(), e);
                }
            }
        }
        
        count.setStatus("APPROVED");
        count.setApprovedBy(approvedBy);
        count.setApprovedAt(LocalDateTime.now());
        
        return stockCountRepository.save(count);
    }
    
    @Transactional
    public void cancelCount(UUID id) {
        log.info("Cancelling stock count: {}", id);
        
        StockCount count = getCountById(id);
        
        if ("APPROVED".equals(count.getStatus())) {
            throw new RuntimeException("Cannot cancel approved counts");
        }
        
        count.setStatus("CANCELLED");
        stockCountRepository.save(count);
    }
}

