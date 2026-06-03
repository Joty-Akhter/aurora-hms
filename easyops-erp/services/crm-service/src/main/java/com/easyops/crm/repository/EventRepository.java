package com.easyops.crm.repository;

import com.easyops.crm.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    List<Event> findByOrganizationIdOrderByStartDatetimeAsc(UUID organizationId);
    
    List<Event> findByOrganizationIdAndOrganizerId(UUID organizationId, UUID organizerId);
    
    List<Event> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Event> findByLeadId(UUID leadId);
    
    List<Event> findByContactId(UUID contactId);
    
    List<Event> findByAccountId(UUID accountId);
    
    List<Event> findByOpportunityId(UUID opportunityId);
    
    List<Event> findByCampaignId(UUID campaignId);
    
    @Query("SELECT e FROM Event e WHERE e.organizationId = :orgId " +
           "AND e.startDatetime BETWEEN :startDate AND :endDate ORDER BY e.startDatetime ASC")
    List<Event> findEventsBetweenDates(@Param("orgId") UUID organizationId, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e FROM Event e WHERE e.organizationId = :orgId AND e.organizerId = :userId " +
           "AND e.startDatetime BETWEEN :startDate AND :endDate ORDER BY e.startDatetime ASC")
    List<Event> findEventsForUserBetweenDates(@Param("orgId") UUID organizationId,
                                              @Param("userId") UUID userId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}

