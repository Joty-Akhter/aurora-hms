package com.easyops.crm.service;

import com.easyops.crm.entity.Lead;
import com.easyops.crm.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeadService {
    
    private final LeadRepository leadRepository;
    
    public List<Lead> getAllLeads(UUID organizationId) {
        return leadRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    public Lead getLeadById(UUID leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
    }

    public UUID getOrganizationIdForLead(UUID leadId) {
        return getLeadById(leadId).getOrganizationId();
    }
    
    public List<Lead> getLeadsByStatus(UUID organizationId, String status) {
        return leadRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Lead> getLeadsByOwner(UUID organizationId, UUID ownerId) {
        return leadRepository.findByOrganizationIdAndOwnerId(organizationId, ownerId);
    }
    
    public List<Lead> searchLeads(UUID organizationId, String searchTerm) {
        return leadRepository.searchLeads(organizationId, searchTerm);
    }
    
    public Lead createLead(Lead lead) {
        log.info("Creating lead: {} {} from {}", lead.getFirstName(), lead.getLastName(), lead.getCompany());
        
        // Generate lead number if not provided
        if (lead.getLeadNumber() == null || lead.getLeadNumber().isEmpty()) {
            lead.setLeadNumber(generateLeadNumber(lead.getOrganizationId()));
        }
        
        // Set initial status
        if (lead.getStatus() == null) {
            lead.setStatus("NEW");
        }
        
        return leadRepository.save(lead);
    }
    
    public Lead updateLead(UUID leadId, Lead leadUpdate) {
        Lead existing = getLeadById(leadId);
        
        if (leadUpdate.getFirstName() != null) existing.setFirstName(leadUpdate.getFirstName());
        if (leadUpdate.getLastName() != null) existing.setLastName(leadUpdate.getLastName());
        if (leadUpdate.getEmail() != null) existing.setEmail(leadUpdate.getEmail());
        if (leadUpdate.getPhone() != null) existing.setPhone(leadUpdate.getPhone());
        if (leadUpdate.getMobile() != null) existing.setMobile(leadUpdate.getMobile());
        if (leadUpdate.getCompany() != null) existing.setCompany(leadUpdate.getCompany());
        if (leadUpdate.getJobTitle() != null) existing.setJobTitle(leadUpdate.getJobTitle());
        if (leadUpdate.getStatus() != null) existing.setStatus(leadUpdate.getStatus());
        if (leadUpdate.getRating() != null) existing.setRating(leadUpdate.getRating());
        if (leadUpdate.getLeadScore() != null) existing.setLeadScore(leadUpdate.getLeadScore());
        if (leadUpdate.getIndustry() != null) existing.setIndustry(leadUpdate.getIndustry());
        if (leadUpdate.getStreet() != null) existing.setStreet(leadUpdate.getStreet());
        if (leadUpdate.getCity() != null) existing.setCity(leadUpdate.getCity());
        if (leadUpdate.getState() != null) existing.setState(leadUpdate.getState());
        if (leadUpdate.getPostalCode() != null) existing.setPostalCode(leadUpdate.getPostalCode());
        if (leadUpdate.getCountry() != null) existing.setCountry(leadUpdate.getCountry());
        if (leadUpdate.getNotes() != null) existing.setNotes(leadUpdate.getNotes());
        
        return leadRepository.save(existing);
    }
    
    public void deleteLead(UUID leadId) {
        leadRepository.deleteById(leadId);
    }
    
    public Lead assignLead(UUID leadId, UUID ownerId) {
        Lead lead = getLeadById(leadId);
        lead.setOwnerId(ownerId);
        lead.setAssignedAt(LocalDateTime.now());
        return leadRepository.save(lead);
    }
    
    public Lead qualifyLead(UUID leadId, UUID qualifiedBy) {
        Lead lead = getLeadById(leadId);
        lead.setStatus("QUALIFIED");
        lead.setQualifiedAt(LocalDateTime.now());
        lead.setQualifiedBy(qualifiedBy);
        return leadRepository.save(lead);
    }
    
    public Lead disqualifyLead(UUID leadId, String reason) {
        Lead lead = getLeadById(leadId);
        lead.setStatus("UNQUALIFIED");
        lead.setDisqualifiedReason(reason);
        return leadRepository.save(lead);
    }
    
    public Map<String, Object> convertLead(UUID leadId, UUID convertedBy, UUID accountId, UUID contactId, UUID opportunityId) {
        Lead lead = getLeadById(leadId);
        
        lead.setStatus("CONVERTED");
        lead.setConvertedAt(LocalDateTime.now());
        lead.setConvertedBy(convertedBy);
        lead.setConvertedAccountId(accountId);
        lead.setConvertedContactId(contactId);
        lead.setConvertedOpportunityId(opportunityId);
        
        leadRepository.save(lead);
        
        Map<String, Object> result = new HashMap<>();
        result.put("leadId", leadId);
        result.put("accountId", accountId);
        result.put("contactId", contactId);
        result.put("opportunityId", opportunityId);
        result.put("convertedAt", lead.getConvertedAt());
        
        return result;
    }
    
    public Map<String, Object> getDashboardStats(UUID organizationId) {
        List<Lead> allLeads = leadRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        
        long totalLeads = allLeads.size();
        long newLeads = allLeads.stream().filter(l -> "NEW".equals(l.getStatus())).count();
        long qualifiedLeads = allLeads.stream().filter(l -> "QUALIFIED".equals(l.getStatus())).count();
        long convertedLeads = allLeads.stream().filter(l -> "CONVERTED".equals(l.getStatus())).count();
        
        double conversionRate = totalLeads > 0 ? (convertedLeads * 100.0 / totalLeads) : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLeads", totalLeads);
        stats.put("newLeads", newLeads);
        stats.put("qualifiedLeads", qualifiedLeads);
        stats.put("convertedLeads", convertedLeads);
        stats.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        
        return stats;
    }
    
    private String generateLeadNumber(UUID organizationId) {
        // Simple lead number generation
        long count = leadRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).size();
        return String.format("LEAD-%06d", count + 1);
    }
}


