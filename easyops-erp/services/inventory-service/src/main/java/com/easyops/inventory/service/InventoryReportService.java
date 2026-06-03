package com.easyops.inventory.service;

import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.entity.StockMovement;
import com.easyops.inventory.repository.StockMovementRepository;
import com.easyops.inventory.repository.StockRepository;
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
public class InventoryReportService {
    
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    
    /**
     * Stock summary report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStockSummaryReport(UUID organizationId) {
        log.debug("Generating stock summary report for organization: {}", organizationId);
        
        List<Stock> allStock = stockRepository.findByOrganizationId(organizationId);
        
        BigDecimal totalValue = allStock.stream()
            .map(Stock::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalOnHand = allStock.stream()
            .map(Stock::getQuantityOnHand)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAllocated = allStock.stream()
            .map(Stock::getQuantityAllocated)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAvailable = allStock.stream()
            .map(Stock::getQuantityAvailable)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int outOfStock = (int) allStock.stream()
            .filter(s -> s.getQuantityAvailable().compareTo(BigDecimal.ZERO) <= 0)
            .count();
        
        int lowStock = (int) stockRepository.findItemsAtReorderPoint(organizationId).size();
        
        Map<String, Object> report = new HashMap<>();
        report.put("organizationId", organizationId);
        report.put("reportDate", LocalDate.now());
        report.put("totalSKUs", allStock.size());
        report.put("totalValue", totalValue);
        report.put("totalQuantityOnHand", totalOnHand);
        report.put("totalQuantityAllocated", totalAllocated);
        report.put("totalQuantityAvailable", totalAvailable);
        report.put("outOfStockItems", outOfStock);
        report.put("lowStockItems", lowStock);
        
        return report;
    }
    
    /**
     * Stock movement report for a date range
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStockMovementReport(
            UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.debug("Generating stock movement report from {} to {}", startDate, endDate);
        
        List<StockMovement> movements = movementRepository.findByDateRange(organizationId, startDate, endDate);
        
        List<Map<String, Object>> report = new ArrayList<>();
        
        for (StockMovement movement : movements) {
            Map<String, Object> item = new HashMap<>();
            item.put("transactionNumber", movement.getTransactionNumber());
            item.put("transactionDate", movement.getTransactionDate());
            item.put("transactionType", movement.getTransactionType());
            item.put("productId", movement.getProductId());
            item.put("warehouseId", movement.getWarehouseId());
            item.put("quantity", movement.getQuantity());
            item.put("unitCost", movement.getUnitCost());
            item.put("totalCost", movement.getTotalCost());
            item.put("sourceType", movement.getSourceType());
            item.put("status", movement.getStatus());
            
            report.add(item);
        }
        
        return report;
    }
    
    /**
     * Inventory turnover report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryTurnoverReport(UUID organizationId, int days) {
        log.debug("Calculating inventory turnover for {} days", days);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        // Get all issue movements (sales)
        List<StockMovement> issues = movementRepository.findByDateRange(organizationId, startDate, endDate).stream()
            .filter(m -> "ISSUE".equals(m.getTransactionType()))
            .toList();
        
        BigDecimal totalCOGS = issues.stream()
            .map(StockMovement::getTotalCost)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get average inventory value
        List<Stock> currentStock = stockRepository.findByOrganizationId(organizationId);
        BigDecimal avgInventoryValue = currentStock.stream()
            .map(Stock::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate turnover ratio
        BigDecimal turnoverRatio = BigDecimal.ZERO;
        if (avgInventoryValue.compareTo(BigDecimal.ZERO) > 0) {
            turnoverRatio = totalCOGS.divide(avgInventoryValue, 4, RoundingMode.HALF_UP);
        }
        
        // Annualize if period is less than a year
        BigDecimal annualizedTurnover = turnoverRatio.multiply(BigDecimal.valueOf(365.0 / days))
            .setScale(2, RoundingMode.HALF_UP);
        
        Map<String, Object> report = new HashMap<>();
        report.put("periodDays", days);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalCOGS", totalCOGS);
        report.put("avgInventoryValue", avgInventoryValue);
        report.put("turnoverRatio", turnoverRatio);
        report.put("annualizedTurnover", annualizedTurnover);
        report.put("daysOfInventory", turnoverRatio.compareTo(BigDecimal.ZERO) > 0 
            ? BigDecimal.valueOf(days).divide(turnoverRatio, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
        
        return report;
    }
    
    /**
     * ABC Analysis - Classify inventory by value
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getABCAnalysis(UUID organizationId) {
        log.debug("Performing ABC analysis for organization: {}", organizationId);
        
        List<Stock> allStock = stockRepository.findByOrganizationId(organizationId);
        
        // Sort by total cost descending
        allStock.sort((a, b) -> b.getTotalCost().compareTo(a.getTotalCost()));
        
        BigDecimal totalValue = allStock.stream()
            .map(Stock::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Map<String, Object>> analysis = new ArrayList<>();
        BigDecimal cumulativeValue = BigDecimal.ZERO;
        
        for (Stock stock : allStock) {
            cumulativeValue = cumulativeValue.add(stock.getTotalCost());
            BigDecimal cumulativePercent = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? cumulativeValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            
            String classification;
            if (cumulativePercent.compareTo(BigDecimal.valueOf(80)) <= 0) {
                classification = "A";
            } else if (cumulativePercent.compareTo(BigDecimal.valueOf(95)) <= 0) {
                classification = "B";
            } else {
                classification = "C";
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("productId", stock.getProductId());
            item.put("warehouseId", stock.getWarehouseId());
            item.put("quantityOnHand", stock.getQuantityOnHand());
            item.put("unitCost", stock.getUnitCost());
            item.put("totalValue", stock.getTotalCost());
            item.put("percentOfTotal", stock.getTotalCost().divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            item.put("cumulativePercent", cumulativePercent);
            item.put("classification", classification);
            
            analysis.add(item);
        }
        
        return analysis;
    }
}

