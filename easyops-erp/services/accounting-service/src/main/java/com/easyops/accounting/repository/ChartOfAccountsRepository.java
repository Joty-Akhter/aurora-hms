package com.easyops.accounting.repository;

import com.easyops.accounting.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChartOfAccountsRepository extends JpaRepository<ChartOfAccounts, UUID> {
    
    List<ChartOfAccounts> findByOrganizationIdAndIsActiveOrderByAccountCode(UUID organizationId, Boolean isActive);
    
    List<ChartOfAccounts> findByOrganizationIdOrderByAccountCode(UUID organizationId);
    
    Optional<ChartOfAccounts> findByOrganizationIdAndAccountCode(UUID organizationId, String accountCode);
    
    List<ChartOfAccounts> findByOrganizationIdAndAccountType(UUID organizationId, String accountType);
    
    List<ChartOfAccounts> findByOrganizationIdAndParentAccountId(UUID organizationId, UUID parentAccountId);
    
    @Query("SELECT c FROM ChartOfAccounts c WHERE c.organizationId = :orgId AND c.isGroup = false AND c.isActive = true ORDER BY c.accountCode")
    List<ChartOfAccounts> findPostingAccounts(@Param("orgId") UUID organizationId);
    
    @Query("SELECT c FROM ChartOfAccounts c WHERE c.organizationId = :orgId AND c.level = :level ORDER BY c.accountCode")
    List<ChartOfAccounts> findByLevel(@Param("orgId") UUID organizationId, @Param("level") Integer level);
    
    boolean existsByOrganizationIdAndAccountCode(UUID organizationId, String accountCode);
    
    long countByOrganizationId(UUID organizationId);
}

