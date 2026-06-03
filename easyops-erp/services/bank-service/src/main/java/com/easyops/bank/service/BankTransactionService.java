package com.easyops.bank.service;

import com.easyops.bank.dto.TransactionRequest;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.entity.BankTransaction;
import com.easyops.bank.repository.BankAccountRepository;
import com.easyops.bank.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransactionService {
    private final BankTransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;
    private final BankGlJournalPostingService bankGlJournalPostingService;
    
    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionsByAccount(UUID accountId) {
        return transactionRepository.findByBankAccountId(accountId);
    }
    
    @Transactional(readOnly = true)
    public List<BankTransaction> getUnreconciledTransactions(UUID accountId) {
        return transactionRepository.findUnreconciledTransactions(accountId);
    }
    
    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionsByDateRange(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByAccountAndDateRange(accountId, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public BankTransaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + id));
    }
    
    @Transactional
    public BankTransaction createTransaction(TransactionRequest request, UUID actorUserId) {
        BankAccount account = accountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        
        BankTransaction transaction = new BankTransaction();
        transaction.setBankAccount(account);
        transaction.setBankAccountId(account.getId());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setReferenceNumber(request.getReferenceNumber());
        transaction.setDescription(request.getDescription());
        transaction.setDebitAmount(request.getDebitAmount());
        transaction.setCreditAmount(request.getCreditAmount());
        transaction.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : actorUserId);
        transaction.setStatus("POSTED");
        transaction.setIsReconciled(false);
        
        // Calculate running balance
        BigDecimal netAmount = request.getCreditAmount().subtract(request.getDebitAmount());
        BigDecimal newBalance = account.getCurrentBalance().add(netAmount);
        transaction.setRunningBalance(newBalance);
        
        transaction = transactionRepository.save(transaction);

        if (Boolean.TRUE.equals(request.getPostToGl())) {
            UUID journalId = bankGlJournalPostingService.postTransactionJournal(
                    transaction, account, request.getOffsetGlAccountId(), actorUserId);
            transaction.setGlJournalId(journalId);
            transaction = transactionRepository.save(transaction);
        } else if (request.getGlJournalId() != null) {
            transaction.setGlJournalId(request.getGlJournalId());
            transaction = transactionRepository.save(transaction);
        }
        
        // Update account balance
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);
        
        return transaction;
    }
    
    @Transactional
    public void deleteTransaction(UUID id) {
        BankTransaction transaction = getTransactionById(id);
        if (transaction.getIsReconciled()) {
            throw new RuntimeException("Cannot delete reconciled transaction");
        }
        transactionRepository.deleteById(id);
    }
}

