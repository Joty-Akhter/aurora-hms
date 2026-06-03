package com.easyops.crm.service;

import com.easyops.crm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CrmAnalyticsService {
    
    @Autowired
    private LeadRepository leadRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private OpportunityRepository opportunityRepository;
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    public Map<String, Object> getOverallDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Leads
        long totalLeads = leadRepository.count();
        long newLeads = leadRepository.findByOrganizationIdAndStatus(organizationId, "NEW").size();
        long convertedLeads = leadRepository.findByOrganizationIdAndStatus(organizationId, "CONVERTED").size();
        
        stats.put("totalLeads", totalLeads);
        stats.put("newLeads", newLeads);
        stats.put("convertedLeads", convertedLeads);
        
        if (totalLeads > 0) {
            stats.put("conversionRate", Math.round((double) convertedLeads / totalLeads * 100 * 100.0) / 100.0);
        } else {
            stats.put("conversionRate", 0.0);
        }
        
        // Accounts & Contacts
        stats.put("totalAccounts", accountRepository.count());
        
        // Opportunities
        long openOpportunities = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "OPEN");
        long wonOpportunities = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "WON");
        long lostOpportunities = opportunityRepository.countByOrganizationIdAndStatus(organizationId, "LOST");
        
        stats.put("openOpportunities", openOpportunities);
        stats.put("wonOpportunities", wonOpportunities);
        stats.put("lostOpportunities", lostOpportunities);
        
        long totalClosed = wonOpportunities + lostOpportunities;
        if (totalClosed > 0) {
            stats.put("winRate", Math.round((double) wonOpportunities / totalClosed * 100 * 100.0) / 100.0);
        } else {
            stats.put("winRate", 0.0);
        }
        
        // Cases
        long newCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "NEW");
        long openCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "OPEN");
        long resolvedCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "RESOLVED");
        
        stats.put("newCases", newCases);
        stats.put("openCases", openCases);
        stats.put("activeCases", newCases + openCases);
        stats.put("resolvedCases", resolvedCases);
        
        // Campaigns
        long activeCampaigns = campaignRepository.findByOrganizationIdAndStatus(organizationId, "ACTIVE").size();
        stats.put("activeCampaigns", activeCampaigns);
        
        // Tasks & Events
        long openTasks = taskRepository.findByOrganizationIdAndStatus(organizationId, "NOT_STARTED").size() +
                        taskRepository.findByOrganizationIdAndStatus(organizationId, "IN_PROGRESS").size();
        stats.put("openTasks", openTasks);
        
        // Knowledge Base
        long publishedArticles = knowledgeBaseRepository.findByOrganizationIdAndStatus(organizationId, "PUBLISHED").size();
        stats.put("publishedArticles", publishedArticles);
        
        return stats;
    }
    
    public Map<String, Object> getLeadAnalytics(UUID organizationId) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Add lead-specific analytics
        long totalLeads = leadRepository.count();
        analytics.put("totalLeads", totalLeads);
        
        return analytics;
    }
    
    public Map<String, Object> getOpportunityAnalytics(UUID organizationId) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Add opportunity-specific analytics
        
        return analytics;
    }
    
    public Map<String, Object> getSupportAnalytics(UUID organizationId) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Add support-specific analytics
        
        return analytics;
    }
}

