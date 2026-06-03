package com.easyops.crm.repository;

import com.easyops.crm.entity.OpportunityActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OpportunityActivityRepository extends JpaRepository<OpportunityActivity, UUID> {
    
    List<OpportunityActivity> findByOpportunityIdOrderByActivityDateDesc(UUID opportunityId);
    
    List<OpportunityActivity> findByOrganizationIdAndAssignedTo(UUID organizationId, UUID assignedTo);
    
    List<OpportunityActivity> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    @Query("SELECT oa FROM OpportunityActivity oa WHERE oa.opportunityId = :oppId " +
           "AND oa.activityType = :type ORDER BY oa.activityDate DESC")
    List<OpportunityActivity> findByOpportunityIdAndType(@Param("oppId") UUID opportunityId, 
                                                         @Param("type") String activityType);
    
    @Query("SELECT oa FROM OpportunityActivity oa WHERE oa.organizationId = :orgId " +
           "AND oa.activityDate BETWEEN :startDate AND :endDate ORDER BY oa.activityDate DESC")
    List<OpportunityActivity> findActivitiesBetweenDates(@Param("orgId") UUID organizationId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
}

