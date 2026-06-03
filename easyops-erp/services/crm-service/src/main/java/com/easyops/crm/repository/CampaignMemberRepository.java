package com.easyops.crm.repository;

import com.easyops.crm.entity.CampaignMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {
    
    List<CampaignMember> findByCampaignId(UUID campaignId);
    
    List<CampaignMember> findByCampaignIdAndMemberStatus(UUID campaignId, String memberStatus);
    
    List<CampaignMember> findByLeadId(UUID leadId);
    
    List<CampaignMember> findByContactId(UUID contactId);
    
    List<CampaignMember> findByAccountId(UUID accountId);
    
    @Query("SELECT COUNT(cm) FROM CampaignMember cm WHERE cm.campaignId = :campaignId")
    long countByCampaignId(@Param("campaignId") UUID campaignId);
    
    @Query("SELECT COUNT(cm) FROM CampaignMember cm WHERE cm.campaignId = :campaignId AND cm.responded = true")
    long countRespondedByCampaignId(@Param("campaignId") UUID campaignId);
    
    @Query("SELECT COUNT(cm) FROM CampaignMember cm WHERE cm.campaignId = :campaignId AND cm.converted = true")
    long countConvertedByCampaignId(@Param("campaignId") UUID campaignId);
}

