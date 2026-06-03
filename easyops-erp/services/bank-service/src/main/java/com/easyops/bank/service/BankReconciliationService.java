package com.easyops.bank.service;

import com.easyops.bank.dto.ReconciliationRequest;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.entity.BankReconciliation;
import com.easyops.bank.entity.BankTransaction;
import com.easyops.bank.entity.ReconciliationItem;
import com.easyops.bank.repository.BankAccountRepository;
import com.easyops.bank.repository.BankReconciliationRepository;
import com.easyops.bank.repository.BankTransactionRepository;
import com.easyops.bank.repository.ReconciliationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationService {
    private final BankReconciliationRepository reconciliationRepository;
    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final ReconciliationItemRepository reconciliationItemRepository;
    
    @Transactional(readOnly = true)
    public List<BankReconciliation> getReconciliationsByAccount(UUID accountId) {
        return reconciliationRepository.findByBankAccountId(accountId);
    }
    
    @Transactional(readOnly = true)
    public BankReconciliation getReconciliationById(UUID id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found with ID: " + id));
    }
    
    @Transactional
    public BankReconciliation createReconciliation(ReconciliationRequest request) {
        BankAccount account = accountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        
        BankReconciliation reconciliation = new BankReconciliation();
        reconciliation.setBankAccount(account);
        reconciliation.setStatementDate(request.getStatementDate());
        reconciliation.setOpeningBalance(request.getOpeningBalance());
        reconciliation.setClosingBalance(request.getClosingBalance());
        reconciliation.setBookBalance(account.getCurrentBalance());
        
        BigDecimal difference = request.getClosingBalance().subtract(account.getCurrentBalance());
        reconciliation.setDifference(difference);
        reconciliation.setStatus("IN_PROGRESS");
        reconciliation.setNotes(request.getNotes());
        
        BankReconciliation savedReconciliation = reconciliationRepository.save(reconciliation);
        
        // Mark selected transactions as reconciled
        for (UUID transactionId : request.getMatchedTransactionIds()) {
            BankTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
            
            transaction.setIsReconciled(true);
            transaction.setReconciliationId(savedReconciliation.getId());
            transactionRepository.save(transaction);
            
            ReconciliationItem item = new ReconciliationItem();
            item.setReconciliation(savedReconciliation);
            item.setTransaction(transaction);
            item.setIsMatched(true);
            item.setMatchType("MANUAL");
            reconciliationItemRepository.save(item);
        }
        
        return savedReconciliation;
    }
    
    @Transactional
    public BankReconciliation completeReconciliation(UUID id, UUID userId) {
        BankReconciliation reconciliation = getReconciliationById(id);
        
        if (!"IN_PROGRESS".equals(reconciliation.getStatus())) {
            throw new RuntimeException("Only in-progress reconciliations can be completed");
        }
        
        if (reconciliation.getDifference().compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("Cannot complete reconciliation with non-zero difference");
        }
        
        reconciliation.setStatus("COMPLETED");
        reconciliation.setReconciledBy(userId);
        reconciliation.setReconciledAt(LocalDateTime.now());
        
        return reconciliationRepository.save(reconciliation);
    }
    
    @Transactional
    public void deleteReconciliation(UUID id) {
        BankReconciliation reconciliation = getReconciliationById(id);
        
        if ("COMPLETED".equals(reconciliation.getStatus())) {
            throw new RuntimeException("Cannot delete completed reconciliation");
        }
        
        // Unmark transactions
        List<ReconciliationItem> items = reconciliationItemRepository.findByReconciliationId(id);
        for (ReconciliationItem item : items) {
            BankTransaction transaction = item.getTransaction();
            transaction.setIsReconciled(false);
            transaction.setReconciliationId(null);
            transactionRepository.save(transaction);
        }
        
        reconciliationItemRepository.deleteByReconciliationId(id);
        reconciliationRepository.deleteById(id);
    }
}

