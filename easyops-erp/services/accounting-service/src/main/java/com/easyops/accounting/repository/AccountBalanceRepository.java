package com.easyops.accounting.repository;

import com.easyops.accounting.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    
    Optional<AccountBalance> findByAccountIdAndPeriodId(UUID accountId, UUID periodId);
    
    List<AccountBalance> findByPeriodIdOrderByAccountId(UUID periodId);
    
    List<AccountBalance> findByOrganizationIdAndPeriodId(UUID organizationId, UUID periodId);
    
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId IN :accountIds AND ab.periodId = :periodId")
    List<AccountBalance> findByAccountIdsAndPeriod(@Param("accountIds") List<UUID> accountIds, @Param("periodId") UUID periodId);
}

