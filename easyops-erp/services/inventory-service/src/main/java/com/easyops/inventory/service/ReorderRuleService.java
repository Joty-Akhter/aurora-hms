package com.easyops.inventory.service;

import com.easyops.inventory.entity.ReorderAlert;
import com.easyops.inventory.entity.ReorderRule;
import com.easyops.inventory.repository.ReorderAlertRepository;
import com.easyops.inventory.repository.ReorderRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReorderRuleService {
    
    private final ReorderRuleRepository reorderRuleRepository;
    private final ReorderAlertRepository reorderAlertRepository;
    private final ReorderMonitoringService monitoringService;
    
    @Transactional(readOnly = true)
    public List<ReorderRule> getAllRules(UUID organizationId) {
        log.debug("Fetching all reorder rules for organization: {}", organizationId);
        return reorderRuleRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ReorderRule> getActiveRules(UUID organizationId) {
        log.debug("Fetching active reorder rules for organization: {}", organizationId);
        return reorderRuleRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public ReorderRule getRuleById(UUID id) {
        log.debug("Fetching reorder rule by ID: {}", id);
        return reorderRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reorder rule not found with ID: " + id));
    }
    
    @Transactional
    public ReorderRule createRule(ReorderRule rule) {
        log.info("Creating reorder rule for product: {}, warehouse: {}", 
                 rule.getProductId(), rule.getWarehouseId());
        
        if (reorderRuleRepository.existsByOrganizationIdAndProductIdAndWarehouseId(
                rule.getOrganizationId(), rule.getProductId(), rule.getWarehouseId())) {
            throw new RuntimeException("Reorder rule already exists for this product/warehouse combination");
        }
        
        return reorderRuleRepository.save(rule);
    }
    
    @Transactional
    public ReorderRule updateRule(UUID id, ReorderRule updatedRule) {
        log.info("Updating reorder rule: {}", id);
        
        ReorderRule rule = getRuleById(id);
        rule.setReorderPoint(updatedRule.getReorderPoint());
        rule.setReorderQuantity(updatedRule.getReorderQuantity());
        rule.setMinQuantity(updatedRule.getMinQuantity());
        rule.setMaxQuantity(updatedRule.getMaxQuantity());
        rule.setLeadTimeDays(updatedRule.getLeadTimeDays());
        rule.setSafetyStock(updatedRule.getSafetyStock());
        rule.setPreferredSupplierId(updatedRule.getPreferredSupplierId());
        rule.setIsActive(updatedRule.getIsActive());
        rule.setAutoCreatePo(updatedRule.getAutoCreatePo());
        rule.setNotes(updatedRule.getNotes());
        
        return reorderRuleRepository.save(rule);
    }
    
    @Transactional
    public void deleteRule(UUID id) {
        log.info("Deleting reorder rule: {}", id);
        reorderRuleRepository.deleteById(id);
    }
    
    @Transactional
    public void checkRuleNow(UUID id) {
        log.info("Manual check of reorder rule: {}", id);
        ReorderRule rule = getRuleById(id);
        monitoringService.checkAndGenerateAlert(rule);
    }
    
    // Alert management
    
    @Transactional(readOnly = true)
    public List<ReorderAlert> getAllAlerts(UUID organizationId) {
        return reorderAlertRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ReorderAlert> getOpenAlerts(UUID organizationId) {
        return reorderAlertRepository.findOpenAlerts(organizationId);
    }
    
    @Transactional(readOnly = true)
    public ReorderAlert getAlertById(UUID id) {
        return reorderAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reorder alert not found with ID: " + id));
    }
    
    @Transactional
    public ReorderAlert acknowledgeAlert(UUID alertId, UUID userId) {
        log.info("Acknowledging alert: {} by user: {}", alertId, userId);
        
        ReorderAlert alert = getAlertById(alertId);
        alert.setAlertStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        
        return reorderAlertRepository.save(alert);
    }
    
    @Transactional
    public ReorderAlert closeAlert(UUID alertId, String notes) {
        log.info("Closing alert: {}", alertId);
        
        ReorderAlert alert = getAlertById(alertId);
        alert.setAlertStatus("CLOSED");
        alert.setClosedAt(LocalDateTime.now());
        if (notes != null) {
            alert.setNotes(notes);
        }
        
        return reorderAlertRepository.save(alert);
    }
}

