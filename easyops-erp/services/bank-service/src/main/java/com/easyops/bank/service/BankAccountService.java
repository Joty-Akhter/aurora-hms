package com.easyops.bank.service;

import com.easyops.bank.dto.BankAccountRequest;
import com.easyops.bank.entity.BankAccount;
import com.easyops.bank.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    
    @Transactional(readOnly = true)
    public List<BankAccount> getAllAccounts(UUID organizationId) {
        return bankAccountRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<BankAccount> getActiveAccounts(UUID organizationId) {
        return bankAccountRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public BankAccount getAccountById(UUID id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
    }
    
    @Transactional
    public BankAccount createAccount(BankAccountRequest request) {
        if (bankAccountRepository.existsByOrganizationIdAndAccountNumber(
                request.getOrganizationId(), request.getAccountNumber())) {
            throw new RuntimeException("Account number already exists: " + request.getAccountNumber());
        }
        
        BankAccount account = new BankAccount();
        account.setOrganizationId(request.getOrganizationId());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountName(request.getAccountName());
        account.setBankName(request.getBankName());
        account.setBranchName(request.getBranchName());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());
        account.setGlAccountId(request.getGlAccountId());
        account.setOpeningBalance(request.getOpeningBalance());
        account.setCurrentBalance(request.getOpeningBalance());
        account.setIsActive(request.getIsActive());
        
        return bankAccountRepository.save(account);
    }
    
    @Transactional
    public BankAccount updateAccount(UUID id, BankAccountRequest request) {
        BankAccount account = getAccountById(id);
        account.setAccountName(request.getAccountName());
        account.setBankName(request.getBankName());
        account.setBranchName(request.getBranchName());
        account.setAccountType(request.getAccountType());
        account.setIsActive(request.getIsActive());
        return bankAccountRepository.save(account);
    }
    
    @Transactional
    public void deleteAccount(UUID id) {
        bankAccountRepository.deleteById(id);
    }
}

