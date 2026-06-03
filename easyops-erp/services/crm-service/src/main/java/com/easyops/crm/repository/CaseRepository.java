package com.easyops.crm.repository;

import com.easyops.crm.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<Case, UUID> {
    
    List<Case> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<Case> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Case> findByOrganizationIdAndPriority(UUID organizationId, String priority);
    
    List<Case> findByOrganizationIdAndAssignedTo(UUID organizationId, UUID assignedTo);
    
    List<Case> findByContactId(UUID contactId);
    
    List<Case> findByAccountId(UUID accountId);
    
    Optional<Case> findByOrganizationIdAndCaseNumber(UUID organizationId, String caseNumber);
    
    boolean existsByOrganizationIdAndCaseNumber(UUID organizationId, String caseNumber);
    
    @Query("SELECT c FROM Case c WHERE c.organizationId = :orgId " +
           "AND (LOWER(c.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.caseNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Case> searchCases(@Param("orgId") UUID organizationId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT c FROM Case c WHERE c.organizationId = :orgId " +
           "AND c.status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND c.slaBreached = true")
    List<Case> findSlaBreachedCases(@Param("orgId") UUID organizationId);
    
    @Query("SELECT c FROM Case c WHERE c.organizationId = :orgId " +
           "AND c.status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND c.resolutionDue < :currentTime")
    List<Case> findOverdueCases(@Param("orgId") UUID organizationId, @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT COUNT(c) FROM Case c WHERE c.organizationId = :orgId AND c.status = :status")
    long countByOrganizationIdAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);
}

