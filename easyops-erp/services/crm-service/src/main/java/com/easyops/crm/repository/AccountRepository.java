package com.easyops.crm.repository;

import com.easyops.crm.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    List<Account> findByOrganizationIdOrderByAccountNameAsc(UUID organizationId);
    
    List<Account> findByOrganizationIdAndAccountType(UUID organizationId, String accountType);
    
    List<Account> findByOrganizationIdAndOwnerId(UUID organizationId, UUID ownerId);
    
    List<Account> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Account> findByParentAccountId(UUID parentAccountId);
    
    @Query("SELECT a FROM Account a WHERE a.organizationId = :organizationId " +
           "AND (LOWER(a.accountName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY a.accountName ASC")
    List<Account> searchAccounts(UUID organizationId, String searchTerm);
}


