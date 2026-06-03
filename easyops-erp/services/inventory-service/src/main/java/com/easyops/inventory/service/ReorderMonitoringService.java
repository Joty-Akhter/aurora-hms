package com.easyops.inventory.service;

import com.easyops.inventory.entity.ReorderAlert;
import com.easyops.inventory.entity.ReorderRule;
import com.easyops.inventory.entity.Stock;
import com.easyops.inventory.repository.ReorderAlertRepository;
import com.easyops.inventory.repository.ReorderRuleRepository;
import com.easyops.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReorderMonitoringService {
    
    private final ReorderRuleRepository reorderRuleRepository;
    private final ReorderAlertRepository reorderAlertRepository;
    private final StockRepository stockRepository;
    
    /**
     * Scheduled task to check reorder points
     * Runs every 15 minutes
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    @Transactional
    public void checkReorderPoints() {
        log.info("Starting reorder point check...");
        
        try {
            List<ReorderRule> activeRules = reorderRuleRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .toList();
            
            log.info("Found {} active reorder rules to check", activeRules.size());
            
            int alertsGenerated = 0;
            for (ReorderRule rule : activeRules) {
                if (checkAndGenerateAlert(rule)) {
                    alertsGenerated++;
                }
            }
            
            log.info("Reorder point check complete. Generated {} new alerts", alertsGenerated);
            
        } catch (Exception e) {
            log.error("Error during reorder point check", e);
        }
    }
    
    /**
     * Check a specific rule and generate alert if needed
     */
    @Transactional
    public boolean checkAndGenerateAlert(ReorderRule rule) {
        try {
            // Get current stock level
            Optional<Stock> stockOpt = stockRepository.findMainStock(
                rule.getOrganizationId(), 
                rule.getProductId(), 
                rule.getWarehouseId()
            );
            
            if (stockOpt.isEmpty()) {
                log.debug("No stock found for product {} in warehouse {}", 
                         rule.getProductId(), rule.getWarehouseId());
                return false;
            }
            
            Stock stock = stockOpt.get();
            BigDecimal availableQty = stock.getQuantityAvailable();
            
            // Check if below reorder point
            if (availableQty.compareTo(rule.getReorderPoint()) <= 0) {
                log.info("Stock below reorder point: Product={}, Warehouse={}, Available={}, ReorderPoint={}",
                        rule.getProductId(), rule.getWarehouseId(), availableQty, rule.getReorderPoint());
                
                // Check if alert already exists
                List<ReorderAlert> existingAlerts = reorderAlertRepository
                    .findByOrganizationIdAndProductId(rule.getOrganizationId(), rule.getProductId())
                    .stream()
                    .filter(a -> "OPEN".equals(a.getAlertStatus()))
                    .filter(a -> a.getWarehouseId().equals(rule.getWarehouseId()))
                    .toList();
                
                if (existingAlerts.isEmpty()) {
                    generateAlert(rule, stock, availableQty);
                    return true;
                } else {
                    log.debug("Open alert already exists for this product/warehouse");
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error checking reorder rule for product {}", rule.getProductId(), e);
            return false;
        }
    }
    
    /**
     * Generate a reorder alert
     */
    private void generateAlert(ReorderRule rule, Stock stock, BigDecimal currentQty) {
        ReorderAlert alert = new ReorderAlert();
        alert.setOrganizationId(rule.getOrganizationId());
        alert.setProductId(rule.getProductId());
        alert.setWarehouseId(rule.getWarehouseId());
        alert.setReorderRuleId(rule.getId());
        alert.setCurrentQuantity(currentQty);
        alert.setReorderPoint(rule.getReorderPoint());
        alert.setSuggestedOrderQty(rule.getReorderQuantity());
        alert.setAlertStatus("OPEN");
        alert.setPriority(calculatePriority(currentQty, rule));
        alert.setNotificationSent(false);
        
        reorderAlertRepository.save(alert);
        
        // Update rule trigger info
        rule.setLastTriggeredAt(LocalDateTime.now());
        rule.setTriggerCount(rule.getTriggerCount() + 1);
        reorderRuleRepository.save(rule);
        
        log.info("Generated reorder alert for product {} in warehouse {}",
                rule.getProductId(), rule.getWarehouseId());
    }
    
    /**
     * Calculate alert priority based on stock level
     */
    private String calculatePriority(BigDecimal currentQty, ReorderRule rule) {
        // If below min quantity or safety stock, it's critical
        if (rule.getMinQuantity() != null && currentQty.compareTo(rule.getMinQuantity()) < 0) {
            return "CRITICAL";
        }
        if (rule.getSafetyStock() != null && currentQty.compareTo(rule.getSafetyStock()) < 0) {
            return "HIGH";
        }
        
        // Calculate percentage below reorder point
        BigDecimal reorderPoint = rule.getReorderPoint();
        if (reorderPoint.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentBelow = currentQty.divide(reorderPoint, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            if (percentBelow.compareTo(BigDecimal.valueOf(25)) < 0) {
                return "HIGH";
            } else if (percentBelow.compareTo(BigDecimal.valueOf(50)) < 0) {
                return "MEDIUM";
            }
        }
        
        return "LOW";
    }
    
    /**
     * Get alerts that need notification
     */
    @Transactional(readOnly = true)
    public List<ReorderAlert> getAlertsNeedingNotification() {
        return reorderAlertRepository.findAll().stream()
            .filter(a -> "OPEN".equals(a.getAlertStatus()))
            .filter(a -> Boolean.FALSE.equals(a.getNotificationSent()))
            .toList();
    }
    
    /**
     * Mark alert as notified
     */
    @Transactional
    public void markAlertNotified(UUID alertId) {
        reorderAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setNotificationSent(true);
            reorderAlertRepository.save(alert);
        });
    }
}

