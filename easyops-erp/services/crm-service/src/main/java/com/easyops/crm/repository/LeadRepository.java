package com.easyops.crm.repository;

import com.easyops.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    
    List<Lead> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<Lead> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Lead> findByOrganizationIdAndOwnerId(UUID organizationId, UUID ownerId);
    
    List<Lead> findByOrganizationIdAndLeadSourceId(UUID organizationId, UUID leadSourceId);
    
    List<Lead> findByOrganizationIdAndRating(UUID organizationId, String rating);
    
    List<Lead> findByEmail(String email);
    
    List<Lead> findByCompanyAndOrganizationId(String company, UUID organizationId);
    
    @Query("SELECT l FROM Lead l WHERE l.organizationId = :organizationId " +
           "AND l.createdAt >= :startDate AND l.createdAt <= :endDate " +
           "ORDER BY l.createdAt DESC")
    List<Lead> findLeadsInDateRange(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT l FROM Lead l WHERE l.organizationId = :organizationId " +
           "AND (LOWER(l.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(l.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(l.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY l.createdAt DESC")
    List<Lead> searchLeads(UUID organizationId, String searchTerm);
}


