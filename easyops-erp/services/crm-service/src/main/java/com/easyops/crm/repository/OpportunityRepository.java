package com.easyops.crm.repository;

import com.easyops.crm.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {
    
    List<Opportunity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<Opportunity> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Opportunity> findByOrganizationIdAndStageId(UUID organizationId, UUID stageId);
    
    List<Opportunity> findByOrganizationIdAndOwnerId(UUID organizationId, UUID ownerId);
    
    List<Opportunity> findByOrganizationIdAndAccountId(UUID organizationId, UUID accountId);
    
    Optional<Opportunity> findByOrganizationIdAndOpportunityNumber(UUID organizationId, String opportunityNumber);
    
    boolean existsByOrganizationIdAndOpportunityNumber(UUID organizationId, String opportunityNumber);
    
    @Query("SELECT o FROM Opportunity o WHERE o.organizationId = :orgId AND o.status = 'OPEN' " +
           "AND o.expectedCloseDate BETWEEN :startDate AND :endDate ORDER BY o.expectedCloseDate")
    List<Opportunity> findClosingSoon(@Param("orgId") UUID organizationId, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT o FROM Opportunity o WHERE o.organizationId = :orgId " +
           "AND (LOWER(o.opportunityName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(o.opportunityNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Opportunity> searchOpportunities(@Param("orgId") UUID organizationId, 
                                         @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(o) FROM Opportunity o WHERE o.organizationId = :orgId AND o.status = :status")
    long countByOrganizationIdAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);
}

