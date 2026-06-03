package com.easyops.crm.service;

import com.easyops.crm.entity.Opportunity;
import com.easyops.crm.entity.OpportunityActivity;
import com.easyops.crm.entity.OpportunityProduct;
import com.easyops.crm.repository.OpportunityActivityRepository;
import com.easyops.crm.repository.OpportunityProductRepository;
import com.easyops.crm.repository.OpportunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OpportunityService {
    
    @Autowired
    private OpportunityRepository opportunityRepository;
    
    @Autowired
    private OpportunityProductRepository productRepository;
    
    @Autowired
    private OpportunityActivityRepository activityRepository;
    
    public List<Opportunity> getAllOpportunities(UUID organizationId) {
        return opportunityRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    public Optional<Opportunity> getOpportunityById(UUID opportunityId) {
        return opportunityRepository.findById(opportunityId);
    }

    public UUID getOrganizationIdForOpportunity(UUID opportunityId) {
        return getOpportunityById(opportunityId)
                .map(Opportunity::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
    }

    public UUID getOrganizationIdForOpportunityProduct(UUID productId) {
        return productRepository.findById(productId)
                .map(OpportunityProduct::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }

    public UUID getOrganizationIdForOpportunityActivity(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(OpportunityActivity::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));
    }
    
    public Optional<Opportunity> getOpportunityByNumber(UUID organizationId, String opportunityNumber) {
        return opportunityRepository.findByOrganizationIdAndOpportunityNumber(organizationId, opportunityNumber);
    }
    
    public Opportunity createOpportunity(Opportunity opportunity) {
        if (opportunity.getOpportunityNumber() == null || opportunity.getOpportunityNumber().isEmpty()) {
            opportunity.setOpportunityNumber(generateOpportunityNumber(opportunity.getOrganizationId()));
        }
        return opportunityRepository.save(opportunity);
    }
    
    public Opportunity updateOpportunity(UUID opportunityId, Opportunity opportunityDetails) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> {
                    opportunity.setOpportunityName(opportunityDetails.getOpportunityName());
                    opportunity.setAccountId(opportunityDetails.getAccountId());
                    opportunity.setContactId(opportunityDetails.getContactId());
                    opportunity.setStageId(opportunityDetails.getStageId());
                    opportunity.setType(opportunityDetails.getType());
                    opportunity.setAmount(opportunityDetails.getAmount());
                    opportunity.setCurrency(opportunityDetails.getCurrency());
                    opportunity.setProbability(opportunityDetails.getProbability());
                    opportunity.setCloseDate(opportunityDetails.getCloseDate());
                    opportunity.setExpectedCloseDate(opportunityDetails.getExpectedCloseDate());
                    opportunity.setOwnerId(opportunityDetails.getOwnerId());
                    opportunity.setStatus(opportunityDetails.getStatus());
                    opportunity.setLeadSourceId(opportunityDetails.getLeadSourceId());
                    opportunity.setSourceCampaign(opportunityDetails.getSourceCampaign());
                    opportunity.setDescription(opportunityDetails.getDescription());
                    opportunity.setNextStep(opportunityDetails.getNextStep());
                    opportunity.setCompetitors(opportunityDetails.getCompetitors());
                    opportunity.setPriority(opportunityDetails.getPriority());
                    opportunity.setTags(opportunityDetails.getTags());
                    opportunity.setUpdatedBy(opportunityDetails.getUpdatedBy());
                    return opportunityRepository.save(opportunity);
                })
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
    }
    
    public void deleteOpportunity(UUID opportunityId) {
        opportunityRepository.deleteById(opportunityId);
    }
    
    public List<Opportunity> getOpportunitiesByStatus(UUID organizationId, String status) {
        return opportunityRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Opportunity> getOpportunitiesByStage(UUID organizationId, UUID stageId) {
        return opportunityRepository.findByOrganizationIdAndStageId(organizationId, stageId);
    }
    
    public List<Opportunity> getOpportunitiesByOwner(UUID organizationId, UUID ownerId) {
        return opportunityRepository.findByOrganizationIdAndOwnerId(organizationId, ownerId);
    }
    
    public List<Opportunity> getOpportunitiesByAccount(UUID organizationId, UUID accountId) {
        return opportunityRepository.findByOrganizationIdAndAccountId(organizationId, accountId);
    }
    
    public List<Opportunity> searchOpportunities(UUID organizationId, String searchTerm) {
        return opportunityRepository.searchOpportunities(organizationId, searchTerm);
    }
    
    public List<Opportunity> getClosingSoon(UUID organizationId, int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return opportunityRepository.findClosingSoon(organizationId, startDate, endDate);
    }
    
    public Opportunity moveToStage(UUID opportunityId, UUID stageId) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> {
                    opportunity.setStageId(stageId);
                    opportunity.setDaysInStage(0);
                    return opportunityRepository.save(opportunity);
                })
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
    }
    
    public Opportunity markAsWon(UUID opportunityId, String winDescription) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> {
                    opportunity.setStatus("WON");
                    opportunity.setCloseDate(LocalDate.now());
                    opportunity.setWinDescription(winDescription);
                    return opportunityRepository.save(opportunity);
                })
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
    }
    
    public Opportunity markAsLost(UUID opportunityId, String lossReason, String lossDescription) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> {
                    opportunity.setStatus("LOST");
                    opportunity.setCloseDate(LocalDate.now());
                    opportunity.setLossReason(lossReason);
                    opportunity.setLossDescription(lossDescription);
                    return opportunityRepository.save(opportunity);
                })
                .orElseThrow(() -> new RuntimeException("Opportunity not found with id: " + opportunityId));
    }
    
    // Opportunity Products
    public List<OpportunityProduct> getOpportunityProducts(UUID opportunityId) {
        return productRepository.findByOpportunityIdOrderByLineOrder(opportunityId);
    }
    
    public OpportunityProduct addProduct(OpportunityProduct product) {
        return productRepository.save(product);
    }
    
    public OpportunityProduct updateProduct(UUID productId, OpportunityProduct productDetails) {
        return productRepository.findById(productId)
                .map(product -> {
                    product.setProductCode(productDetails.getProductCode());
                    product.setProductName(productDetails.getProductName());
                    product.setProductDescription(productDetails.getProductDescription());
                    product.setQuantity(productDetails.getQuantity());
                    product.setUnitPrice(productDetails.getUnitPrice());
                    product.setDiscountPercent(productDetails.getDiscountPercent());
                    product.setDiscountAmount(productDetails.getDiscountAmount());
                    product.setTaxPercent(productDetails.getTaxPercent());
                    product.setNotes(productDetails.getNotes());
                    product.setLineOrder(productDetails.getLineOrder());
                    product.setUpdatedBy(productDetails.getUpdatedBy());
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }
    
    public void deleteProduct(UUID productId) {
        productRepository.deleteById(productId);
    }
    
    // Opportunity Activities
    public List<OpportunityActivity> getOpportunityActivities(UUID opportunityId) {
        return activityRepository.findByOpportunityIdOrderByActivityDateDesc(opportunityId);
    }
    
    public OpportunityActivity addActivity(OpportunityActivity activity) {
        return activityRepository.save(activity);
    }
    
    public OpportunityActivity updateActivity(UUID activityId, OpportunityActivity activityDetails) {
        return activityRepository.findById(activityId)
                .map(activity -> {
                    activity.setActivityType(activityDetails.getActivityType());
                    activity.setSubject(activityDetails.getSubject());
                    activity.setDescription(activityDetails.getDescription());
                    activity.setActivityDate(activityDetails.getActivityDate());
                    activity.setDurationMinutes(activityDetails.getDurationMinutes());
                    activity.setStatus(activityDetails.getStatus());
                    activity.setPriority(activityDetails.getPriority());
                    activity.setOutcome(activityDetails.getOutcome());
                    activity.setNextAction(activityDetails.getNextAction());
                    activity.setAssignedTo(activityDetails.getAssignedTo());
                    activity.setCompletedBy(activityDetails.getCompletedBy());
                    activity.setUpdatedBy(activityDetails.getUpdatedBy());
                    return activityRepository.save(activity);
                })
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + activityId));
    }
    
    public void deleteActivity(UUID activityId) {
        activityRepository.deleteById(activityId);
    }
    
    private String generateOpportunityNumber(UUID organizationId) {
        String prefix = "OPP";
        long count = opportunityRepository.count() + 1;
        return String.format("%s-%06d", prefix, count);
    }
}

