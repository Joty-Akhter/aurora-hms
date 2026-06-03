package com.easyops.crm.service;

import com.easyops.crm.entity.Account;
import com.easyops.crm.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    public List<Account> getAllAccounts(UUID organizationId) {
        return accountRepository.findByOrganizationIdOrderByAccountNameAsc(organizationId);
    }
    
    public Account getAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public UUID getOrganizationIdForAccount(UUID accountId) {
        return getAccountById(accountId).getOrganizationId();
    }
    
    public List<Account> getAccountsByType(UUID organizationId, String accountType) {
        return accountRepository.findByOrganizationIdAndAccountType(organizationId, accountType);
    }
    
    public List<Account> searchAccounts(UUID organizationId, String searchTerm) {
        return accountRepository.searchAccounts(organizationId, searchTerm);
    }
    
    public Account createAccount(Account account) {
        log.info("Creating account: {}", account.getAccountName());
        
        // Generate account number if not provided
        if (account.getAccountNumber() == null || account.getAccountNumber().isEmpty()) {
            account.setAccountNumber(generateAccountNumber(account.getOrganizationId()));
        }
        
        return accountRepository.save(account);
    }
    
    public Account updateAccount(UUID accountId, Account accountUpdate) {
        Account existing = getAccountById(accountId);
        
        if (accountUpdate.getAccountName() != null) existing.setAccountName(accountUpdate.getAccountName());
        if (accountUpdate.getAccountType() != null) existing.setAccountType(accountUpdate.getAccountType());
        if (accountUpdate.getIndustry() != null) existing.setIndustry(accountUpdate.getIndustry());
        if (accountUpdate.getPhone() != null) existing.setPhone(accountUpdate.getPhone());
        if (accountUpdate.getEmail() != null) existing.setEmail(accountUpdate.getEmail());
        if (accountUpdate.getWebsite() != null) existing.setWebsite(accountUpdate.getWebsite());
        if (accountUpdate.getBillingStreet() != null) existing.setBillingStreet(accountUpdate.getBillingStreet());
        if (accountUpdate.getBillingCity() != null) existing.setBillingCity(accountUpdate.getBillingCity());
        if (accountUpdate.getBillingState() != null) existing.setBillingState(accountUpdate.getBillingState());
        if (accountUpdate.getBillingPostalCode() != null) existing.setBillingPostalCode(accountUpdate.getBillingPostalCode());
        if (accountUpdate.getBillingCountry() != null) existing.setBillingCountry(accountUpdate.getBillingCountry());
        if (accountUpdate.getDescription() != null) existing.setDescription(accountUpdate.getDescription());
        if (accountUpdate.getNotes() != null) existing.setNotes(accountUpdate.getNotes());
        
        return accountRepository.save(existing);
    }
    
    public void deleteAccount(UUID accountId) {
        accountRepository.deleteById(accountId);
    }
    
    private String generateAccountNumber(UUID organizationId) {
        long count = accountRepository.findByOrganizationIdOrderByAccountNameAsc(organizationId).size();
        return String.format("ACC-%06d", count + 1);
    }
}







