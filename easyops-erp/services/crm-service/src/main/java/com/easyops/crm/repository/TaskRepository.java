package com.easyops.crm.repository;

import com.easyops.crm.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    List<Task> findByOrganizationIdOrderByDueDateAsc(UUID organizationId);
    
    List<Task> findByOrganizationIdAndAssignedTo(UUID organizationId, UUID assignedTo);
    
    List<Task> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Task> findByOrganizationIdAndPriority(UUID organizationId, String priority);
    
    List<Task> findByLeadId(UUID leadId);
    
    List<Task> findByContactId(UUID contactId);
    
    List<Task> findByAccountId(UUID accountId);
    
    List<Task> findByOpportunityId(UUID opportunityId);
    
    List<Task> findByCampaignId(UUID campaignId);
    
    @Query("SELECT t FROM Task t WHERE t.organizationId = :orgId AND t.assignedTo = :userId " +
           "AND t.dueDate = :date AND t.status != 'COMPLETED' ORDER BY t.priority DESC")
    List<Task> findDueTasksForUserOnDate(@Param("orgId") UUID organizationId, 
                                         @Param("userId") UUID userId, 
                                         @Param("date") LocalDate date);
    
    @Query("SELECT t FROM Task t WHERE t.organizationId = :orgId AND t.assignedTo = :userId " +
           "AND t.dueDate < :date AND t.status != 'COMPLETED' ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasksForUser(@Param("orgId") UUID organizationId, 
                                       @Param("userId") UUID userId, 
                                       @Param("date") LocalDate date);
}

