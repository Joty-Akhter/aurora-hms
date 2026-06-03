package com.easyops.crm.service;

import com.easyops.crm.entity.Campaign;
import com.easyops.crm.entity.CampaignMember;
import com.easyops.crm.repository.CampaignMemberRepository;
import com.easyops.crm.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CampaignService {
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignMemberRepository memberRepository;
    
    public List<Campaign> getAllCampaigns(UUID organizationId) {
        return campaignRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    public Optional<Campaign> getCampaignById(UUID campaignId) {
        return campaignRepository.findById(campaignId);
    }

    public UUID getOrganizationIdForCampaign(UUID campaignId) {
        return getCampaignById(campaignId)
                .map(Campaign::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + campaignId));
    }

    public UUID getOrganizationIdForCampaignMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(CampaignMember::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Campaign member not found with id: " + memberId));
    }
    
    public Optional<Campaign> getCampaignByNumber(UUID organizationId, String campaignNumber) {
        return campaignRepository.findByOrganizationIdAndCampaignNumber(organizationId, campaignNumber);
    }
    
    public Campaign createCampaign(Campaign campaign) {
        if (campaign.getCampaignNumber() == null || campaign.getCampaignNumber().isEmpty()) {
            campaign.setCampaignNumber(generateCampaignNumber(campaign.getOrganizationId()));
        }
        return campaignRepository.save(campaign);
    }
    
    public Campaign updateCampaign(UUID campaignId, Campaign campaignDetails) {
        return campaignRepository.findById(campaignId)
                .map(campaign -> {
                    campaign.setCampaignName(campaignDetails.getCampaignName());
                    campaign.setCampaignType(campaignDetails.getCampaignType());
                    campaign.setStatus(campaignDetails.getStatus());
                    campaign.setStartDate(campaignDetails.getStartDate());
                    campaign.setEndDate(campaignDetails.getEndDate());
                    campaign.setBudgetedCost(campaignDetails.getBudgetedCost());
                    campaign.setActualCost(campaignDetails.getActualCost());
                    campaign.setCurrency(campaignDetails.getCurrency());
                    campaign.setExpectedResponseCount(campaignDetails.getExpectedResponseCount());
                    campaign.setExpectedRevenue(campaignDetails.getExpectedRevenue());
                    campaign.setOwnerId(campaignDetails.getOwnerId());
                    campaign.setDescription(campaignDetails.getDescription());
                    campaign.setObjectives(campaignDetails.getObjectives());
                    campaign.setTargetAudience(campaignDetails.getTargetAudience());
                    campaign.setUtmSource(campaignDetails.getUtmSource());
                    campaign.setUtmMedium(campaignDetails.getUtmMedium());
                    campaign.setUtmCampaign(campaignDetails.getUtmCampaign());
                    campaign.setUtmTerm(campaignDetails.getUtmTerm());
                    campaign.setUtmContent(campaignDetails.getUtmContent());
                    campaign.setEmailTemplateId(campaignDetails.getEmailTemplateId());
                    campaign.setTags(campaignDetails.getTags());
                    campaign.setPriority(campaignDetails.getPriority());
                    campaign.setUpdatedBy(campaignDetails.getUpdatedBy());
                    return campaignRepository.save(campaign);
                })
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + campaignId));
    }
    
    public void deleteCampaign(UUID campaignId) {
        campaignRepository.deleteById(campaignId);
    }
    
    public List<Campaign> getCampaignsByStatus(UUID organizationId, String status) {
        return campaignRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Campaign> getCampaignsByType(UUID organizationId, String campaignType) {
        return campaignRepository.findByOrganizationIdAndCampaignType(organizationId, campaignType);
    }
    
    public List<Campaign> searchCampaigns(UUID organizationId, String searchTerm) {
        return campaignRepository.searchCampaigns(organizationId, searchTerm);
    }
    
    public List<Campaign> getActiveCampaigns(UUID organizationId) {
        return campaignRepository.findActiveCampaignsOnDate(organizationId, LocalDate.now());
    }
    
    // Campaign Members
    public List<CampaignMember> getCampaignMembers(UUID campaignId) {
        return memberRepository.findByCampaignId(campaignId);
    }
    
    public CampaignMember addCampaignMember(CampaignMember member) {
        return memberRepository.save(member);
    }
    
    public CampaignMember updateCampaignMember(UUID memberId, CampaignMember memberDetails) {
        return memberRepository.findById(memberId)
                .map(member -> {
                    member.setMemberStatus(memberDetails.getMemberStatus());
                    member.setResponded(memberDetails.getResponded());
                    member.setResponseDate(memberDetails.getResponseDate());
                    member.setConverted(memberDetails.getConverted());
                    member.setConversionDate(memberDetails.getConversionDate());
                    member.setEmailSent(memberDetails.getEmailSent());
                    member.setEmailOpened(memberDetails.getEmailOpened());
                    member.setEmailClicked(memberDetails.getEmailClicked());
                    member.setOpenedAt(memberDetails.getOpenedAt());
                    member.setClickedAt(memberDetails.getClickedAt());
                    member.setNotes(memberDetails.getNotes());
                    member.setUpdatedBy(memberDetails.getUpdatedBy());
                    return memberRepository.save(member);
                })
                .orElseThrow(() -> new RuntimeException("Campaign member not found with id: " + memberId));
    }
    
    public void deleteCampaignMember(UUID memberId) {
        memberRepository.deleteById(memberId);
    }
    
    public Map<String, Object> getCampaignStats(UUID campaignId) {
        Map<String, Object> stats = new HashMap<>();
        long totalMembers = memberRepository.countByCampaignId(campaignId);
        long responded = memberRepository.countRespondedByCampaignId(campaignId);
        long converted = memberRepository.countConvertedByCampaignId(campaignId);
        
        stats.put("totalMembers", totalMembers);
        stats.put("responded", responded);
        stats.put("converted", converted);
        
        if (totalMembers > 0) {
            stats.put("responseRate", Math.round((double) responded / totalMembers * 100 * 100.0) / 100.0);
            stats.put("conversionRate", Math.round((double) converted / totalMembers * 100 * 100.0) / 100.0);
        } else {
            stats.put("responseRate", 0.0);
            stats.put("conversionRate", 0.0);
        }
        
        return stats;
    }
    
    private String generateCampaignNumber(UUID organizationId) {
        String prefix = "CMP";
        long count = campaignRepository.count() + 1;
        return String.format("%s-%06d", prefix, count);
    }
}

