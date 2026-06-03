package com.easyops.crm.repository;

import com.easyops.crm.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    
    List<Contact> findByOrganizationIdOrderByLastNameAsc(UUID organizationId);
    
    List<Contact> findByAccountId(UUID accountId);
    
    List<Contact> findByAccountIdAndIsActive(UUID accountId, Boolean isActive);
    
    Optional<Contact> findByAccountIdAndIsPrimary(UUID accountId, Boolean isPrimary);
    
    List<Contact> findByEmail(String email);
    
    @Query("SELECT c FROM Contact c WHERE c.organizationId = :organizationId " +
           "AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.lastName ASC")
    List<Contact> searchContacts(UUID organizationId, String searchTerm);
}


