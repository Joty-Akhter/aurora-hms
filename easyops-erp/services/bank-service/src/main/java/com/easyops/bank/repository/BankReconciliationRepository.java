package com.easyops.bank.repository;

import com.easyops.bank.entity.BankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, UUID> {
    List<BankReconciliation> findByBankAccountId(UUID bankAccountId);
    
    List<BankReconciliation> findByBankAccountIdAndStatus(UUID bankAccountId, String status);
    
    @Query("SELECT r FROM BankReconciliation r WHERE r.bankAccount.id = :accountId " +
           "ORDER BY r.statementDate DESC")
    List<BankReconciliation> findByAccountOrderByDateDesc(@Param("accountId") UUID accountId);
    
    Optional<BankReconciliation> findFirstByBankAccountIdAndStatusOrderByStatementDateDesc(
            UUID bankAccountId, String status);
}

