package com.easyops.crm.repository;

import com.easyops.crm.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    
    List<Campaign> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<Campaign> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Campaign> findByOrganizationIdAndCampaignType(UUID organizationId, String campaignType);
    
    List<Campaign> findByOrganizationIdAndOwnerId(UUID organizationId, UUID ownerId);
    
    Optional<Campaign> findByOrganizationIdAndCampaignNumber(UUID organizationId, String campaignNumber);
    
    boolean existsByOrganizationIdAndCampaignNumber(UUID organizationId, String campaignNumber);
    
    @Query("SELECT c FROM Campaign c WHERE c.organizationId = :orgId " +
           "AND c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)")
    List<Campaign> findActiveCampaignsOnDate(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT c FROM Campaign c WHERE c.organizationId = :orgId " +
           "AND (LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.campaignNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Campaign> searchCampaigns(@Param("orgId") UUID organizationId, @Param("searchTerm") String searchTerm);
}

