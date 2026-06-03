package com.easyops.bank.repository;

import com.easyops.bank.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findByBankAccountId(UUID bankAccountId);
    
    List<BankTransaction> findByBankAccountIdAndIsReconciled(UUID bankAccountId, Boolean isReconciled);
    
    @Query("SELECT t FROM BankTransaction t WHERE t.bankAccount.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.createdAt")
    List<BankTransaction> findByAccountAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM BankTransaction t WHERE t.bankAccount.id = :accountId " +
           "AND t.isReconciled = false ORDER BY t.transactionDate")
    List<BankTransaction> findUnreconciledTransactions(@Param("accountId") UUID accountId);
}

