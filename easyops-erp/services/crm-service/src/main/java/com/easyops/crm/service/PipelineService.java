package com.easyops.crm.service;

import com.easyops.crm.entity.OpportunityStage;
import com.easyops.crm.repository.OpportunityStageRepository;
import com.easyops.crm.repository.OpportunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PipelineService {
    
    @Autowired
    private OpportunityStageRepository stageRepository;
    
    @Autowired
    private OpportunityRepository opportunityRepository;
    
    public List<OpportunityStage> getAllStages(UUID organizationId) {
        return stageRepository.findByOrganizationIdOrderByStageOrder(organizationId);
    }
    
    public List<OpportunityStage> getActiveStages(UUID organizationId) {
        return stageRepository.findByOrganizationIdAndIsActiveOrderByStageOrder(organizationId, true);
    }
    
    public Optional<OpportunityStage> getStageById(UUID stageId) {
        return stageRepository.findById(stageId);
    }

    public UUID getOrganizationIdForStage(UUID stageId) {
        return getStageById(stageId)
                .map(OpportunityStage::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Stage not found with id: " + stageId));
    }
    
    public Optional<OpportunityStage> getStageByCode(UUID organizationId, String stageCode) {
        return stageRepository.findByOrganizationIdAndStageCode(organizationId, stageCode);
    }
    
    public OpportunityStage createStage(OpportunityStage stage) {
        return stageRepository.save(stage);
    }
    
    public OpportunityStage updateStage(UUID stageId, OpportunityStage stageDetails) {
        return stageRepository.findById(stageId)
                .map(stage -> {
                    stage.setStageName(stageDetails.getStageName());
                    stage.setStageCode(stageDetails.getStageCode());
                    stage.setStageOrder(stageDetails.getStageOrder());
                    stage.setProbability(stageDetails.getProbability());
                    stage.setIsClosedWon(stageDetails.getIsClosedWon());
                    stage.setIsClosedLost(stageDetails.getIsClosedLost());
                    stage.setIsActive(stageDetails.getIsActive());
                    stage.setDescription(stageDetails.getDescription());
                    stage.setUpdatedBy(stageDetails.getUpdatedBy());
                    return stageRepository.save(stage);
                })
                .orElseThrow(() -> new RuntimeException("Stage not found with id: " + stageId));
    }
    
    public void deleteStage(UUID stageId) {
        stageRepository.deleteById(stageId);
    }
    
    public Map<String, Object> getPipelineStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Count opportunities by status
        long totalOpen = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "OPEN");
        long totalWon = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "WON");
        long totalLost = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "LOST");
        
        stats.put("totalOpen", totalOpen);
        stats.put("totalWon", totalWon);
        stats.put("totalLost", totalLost);
        stats.put("totalOpportunities", totalOpen + totalWon + totalLost);
        
        // Calculate win rate
        long totalClosed = totalWon + totalLost;
        if (totalClosed > 0) {
            double winRate = (double) totalWon / totalClosed * 100;
            stats.put("winRate", Math.round(winRate * 100.0) / 100.0);
        } else {
            stats.put("winRate", 0.0);
        }
        
        return stats;
    }
}

