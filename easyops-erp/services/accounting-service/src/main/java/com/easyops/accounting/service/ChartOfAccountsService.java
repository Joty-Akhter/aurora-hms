package com.easyops.accounting.service;

import com.easyops.accounting.dto.CoARequest;
import com.easyops.accounting.entity.ChartOfAccounts;
import com.easyops.accounting.repository.ChartOfAccountsRepository;
import com.easyops.accounting.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChartOfAccountsService {
    
    private final ChartOfAccountsRepository coaRepository;
    private final JournalLineRepository journalLineRepository;
    
    @Transactional
    @CacheEvict(value = "coa", key = "#request.organizationId")
    public ChartOfAccounts createAccount(CoARequest request, UUID createdBy) {
        // Validate account code uniqueness
        if (coaRepository.existsByOrganizationIdAndAccountCode(request.getOrganizationId(), request.getAccountCode())) {
            throw new RuntimeException("Account code already exists: " + request.getAccountCode());
        }
        
        // Validate parent account if specified
        if (request.getParentAccountId() != null) {
            ChartOfAccounts parent = coaRepository.findById(request.getParentAccountId())
                .orElseThrow(() -> new RuntimeException("Parent account not found"));
            
            if (!parent.getIsGroup()) {
                throw new RuntimeException("Parent account must be a group account");
            }
        }
        
        ChartOfAccounts account = new ChartOfAccounts();
        account.setOrganizationId(request.getOrganizationId());
        account.setAccountCode(request.getAccountCode());
        account.setAccountName(request.getAccountName());
        account.setParentAccountId(request.getParentAccountId());
        account.setAccountType(request.getAccountType());
        account.setAccountCategory(request.getAccountCategory());
        account.setAccountSubcategory(request.getAccountSubcategory());
        account.setLevel(request.getLevel());
        account.setIsGroup(request.getIsGroup());
        account.setCurrency(request.getCurrency());
        account.setOpeningBalance(request.getOpeningBalance());
        account.setOpeningBalanceDate(request.getOpeningBalanceDate());
        account.setAllowManualEntry(request.getAllowManualEntry());
        account.setDescription(request.getDescription());
        account.setTaxType(request.getTaxType());
        account.setTags(request.getTags());
        account.setCreatedBy(createdBy);
        account.setIsActive(true);
        
        account = coaRepository.save(account);
        log.info("Created account: {} - {}", account.getAccountCode(), account.getAccountName());
        
        return account;
    }
    
    @Cacheable(value = "coa", key = "#organizationId")
    public List<ChartOfAccounts> getOrganizationAccounts(UUID organizationId) {
        // Auto-load standard COA if organization has no accounts
        long count = coaRepository.countByOrganizationId(organizationId);
        if (count == 0) {
            log.info("No accounts found for organization: {}. Auto-loading standard chart of accounts.", organizationId);
            try {
                loadStandardCoA(organizationId, null);
            } catch (Exception e) {
                log.warn("Failed to auto-load standard COA for organization {}: {}", organizationId, e.getMessage());
            }
        }
        
        return coaRepository.findByOrganizationIdOrderByAccountCode(organizationId);
    }
    
    @Cacheable(value = "coa", key = "'active-' + #organizationId")
    public List<ChartOfAccounts> getActiveAccounts(UUID organizationId) {
        return coaRepository.findByOrganizationIdAndIsActiveOrderByAccountCode(organizationId, true);
    }
    
    public List<ChartOfAccounts> getPostingAccounts(UUID organizationId) {
        return coaRepository.findPostingAccounts(organizationId);
    }
    
    public List<ChartOfAccounts> getAccountsByType(UUID organizationId, String accountType) {
        return coaRepository.findByOrganizationIdAndAccountType(organizationId, accountType);
    }
    
    public ChartOfAccounts getAccountByCode(UUID organizationId, String accountCode) {
        return coaRepository.findByOrganizationIdAndAccountCode(organizationId, accountCode)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountCode));
    }
    
    public ChartOfAccounts getAccountById(UUID accountId) {
        return coaRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
    }
    
    @Transactional
    @CacheEvict(value = "coa", key = "#account.organizationId")
    public ChartOfAccounts updateAccount(UUID accountId, CoARequest request, UUID updatedBy) {
        ChartOfAccounts account = getAccountById(accountId);
        
        if (!account.getAccountCode().equals(request.getAccountCode())) {
            if (journalLineRepository.existsPostedOrReversedLinesForAccount(accountId)) {
                throw new RuntimeException(
                        "Cannot change account code: account has posted journal activity. "
                                + "Create a new account and migrate balances instead.");
            }
            if (coaRepository.existsByOrganizationIdAndAccountCode(account.getOrganizationId(), request.getAccountCode())) {
                throw new RuntimeException("Account code already exists: " + request.getAccountCode());
            }
            account.setAccountCode(request.getAccountCode());
            log.info("Updated account code to {}", request.getAccountCode());
        }
        
        account.setAccountName(request.getAccountName());
        account.setAccountCategory(request.getAccountCategory());
        account.setAccountSubcategory(request.getAccountSubcategory());
        account.setDescription(request.getDescription());
        account.setAllowManualEntry(request.getAllowManualEntry());
        account.setTags(request.getTags());
        account.setUpdatedBy(updatedBy);
        
        return coaRepository.save(account);
    }
    
    @Transactional
    @CacheEvict(value = "coa", key = "#organizationId")
    public void deactivateAccount(UUID accountId, UUID organizationId) {
        ChartOfAccounts account = getAccountById(accountId);
        
        // Verify account belongs to organization
        if (!account.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Account does not belong to organization");
        }
        
        // Prevent deactivation of system accounts
        if (account.getIsSystemAccount() != null && account.getIsSystemAccount()) {
            throw new RuntimeException("Cannot deactivate system account. System accounts are required for proper operation.");
        }
        
        account.setIsActive(false);
        coaRepository.save(account);
        log.info("Deactivated account: {}", account.getAccountCode());
    }
    
    @Transactional
    @CacheEvict(value = "coa", key = "#organizationId")
    public int loadStandardCoA(UUID organizationId, UUID createdBy) {
        log.info("Loading standard Chart of Accounts template for organization: {}", organizationId);
        
        // Check if organization already has accounts
        long existingCount = coaRepository.countByOrganizationId(organizationId);
        if (existingCount > 0) {
            log.warn("Organization {} already has {} accounts. Skipping template load.", organizationId, existingCount);
            return 0;
        }
        
        int accountsCreated = 0;
        
        // Create standard Chart of Accounts
        // Cash and Bank Accounts
        accountsCreated += createTemplateAccount(organizationId, "1000", "Cash and Bank Accounts", "ASSET", "Current Assets", 1, true, createdBy, "Group for all cash and bank accounts");
        accountsCreated += createTemplateAccount(organizationId, "1010", "Cash on Hand", "ASSET", "Current Assets", 2, false, createdBy, "Physical cash");
        accountsCreated += createTemplateAccount(organizationId, "1020", "Petty Cash", "ASSET", "Current Assets", 2, false, createdBy, "Petty cash fund");
        accountsCreated += createTemplateAccount(organizationId, "1030", "Bank - Operating Account", "ASSET", "Current Assets", 2, false, createdBy, "Main operating bank account");
        accountsCreated += createTemplateAccount(organizationId, "1040", "Bank - Savings Account", "ASSET", "Current Assets", 2, false, createdBy, "Savings account");
        
        // Accounts Receivable
        accountsCreated += createTemplateAccount(organizationId, "1100", "Accounts Receivable", "ASSET", "Current Assets", 1, true, createdBy, "Group for accounts receivable");
        accountsCreated += createTemplateAccount(organizationId, "1110", "Trade Debtors", "ASSET", "Current Assets", 2, false, createdBy, "Money owed by customers");
        accountsCreated += createTemplateAccount(organizationId, "1120", "Allowance for Doubtful Accounts", "ASSET", "Current Assets", 2, false, createdBy, "Provision for bad debts");
        
        // Inventory
        accountsCreated += createTemplateAccount(organizationId, "1200", "Inventory", "ASSET", "Current Assets", 1, true, createdBy, "Group for inventory accounts");
        accountsCreated += createTemplateAccount(organizationId, "1210", "Raw Materials", "ASSET", "Current Assets", 2, false, createdBy, "Raw materials inventory");
        accountsCreated += createTemplateAccount(organizationId, "1220", "Work in Progress", "ASSET", "Current Assets", 2, false, createdBy, "Work in progress inventory");
        accountsCreated += createTemplateAccount(organizationId, "1230", "Finished Goods", "ASSET", "Current Assets", 2, false, createdBy, "Finished goods inventory");
        
        // Fixed Assets
        accountsCreated += createTemplateAccount(organizationId, "1500", "Fixed Assets", "ASSET", "Fixed Assets", 1, true, createdBy, "Group for fixed assets");
        accountsCreated += createTemplateAccount(organizationId, "1510", "Land and Buildings", "ASSET", "Fixed Assets", 2, false, createdBy, "Land and buildings");
        accountsCreated += createTemplateAccount(organizationId, "1520", "Plant and Machinery", "ASSET", "Fixed Assets", 2, false, createdBy, "Plant and machinery");
        accountsCreated += createTemplateAccount(organizationId, "1530", "Furniture and Fixtures", "ASSET", "Fixed Assets", 2, false, createdBy, "Furniture and fixtures");
        accountsCreated += createTemplateAccount(organizationId, "1540", "Computer Equipment", "ASSET", "Fixed Assets", 2, false, createdBy, "Computer equipment");
        accountsCreated += createTemplateAccount(organizationId, "1550", "Accumulated Depreciation", "ASSET", "Fixed Assets", 2, false, createdBy, "Accumulated depreciation");
        
        // Current Liabilities
        accountsCreated += createTemplateAccount(organizationId, "2000", "Current Liabilities", "LIABILITY", "Current Liabilities", 1, true, createdBy, "Group for current liabilities");
        accountsCreated += createTemplateAccount(organizationId, "2010", "Accounts Payable", "LIABILITY", "Current Liabilities", 2, false, createdBy, "Trade creditors");
        accountsCreated += createTemplateAccount(organizationId, "2020", "Accrued Expenses", "LIABILITY", "Current Liabilities", 2, false, createdBy, "Accrued expenses");
        accountsCreated += createTemplateAccount(organizationId, "2030", "Taxes Payable", "LIABILITY", "Current Liabilities", 2, false, createdBy, "Taxes payable");
        accountsCreated += createTemplateAccount(organizationId, "2040", "Short-term Loans", "LIABILITY", "Current Liabilities", 2, false, createdBy, "Short-term loans");
        
        // Long-term Liabilities
        accountsCreated += createTemplateAccount(organizationId, "2500", "Long-term Liabilities", "LIABILITY", "Long-term Liabilities", 1, true, createdBy, "Group for long-term liabilities");
        accountsCreated += createTemplateAccount(organizationId, "2510", "Long-term Loans", "LIABILITY", "Long-term Liabilities", 2, false, createdBy, "Long-term loans");
        accountsCreated += createTemplateAccount(organizationId, "2520", "Mortgages Payable", "LIABILITY", "Long-term Liabilities", 2, false, createdBy, "Mortgages payable");
        
        // Equity
        accountsCreated += createTemplateAccount(organizationId, "3000", "Equity", "EQUITY", "Owner's Equity", 1, true, createdBy, "Group for equity accounts");
        accountsCreated += createTemplateAccount(organizationId, "3010", "Capital", "EQUITY", "Owner's Equity", 2, false, createdBy, "Owner's capital");
        accountsCreated += createTemplateAccount(organizationId, "3020", "Retained Earnings", "EQUITY", "Owner's Equity", 2, false, createdBy, "Retained earnings");
        accountsCreated += createTemplateAccount(organizationId, "3030", "Current Year Earnings", "EQUITY", "Owner's Equity", 2, false, createdBy, "Current year earnings");
        
        // Revenue
        accountsCreated += createTemplateAccount(organizationId, "4000", "Revenue", "REVENUE", "Operating Revenue", 1, true, createdBy, "Group for revenue accounts");
        accountsCreated += createTemplateAccount(organizationId, "4010", "Sales Revenue", "REVENUE", "Operating Revenue", 2, false, createdBy, "Sales revenue");
        accountsCreated += createTemplateAccount(organizationId, "4020", "Service Revenue", "REVENUE", "Operating Revenue", 2, false, createdBy, "Service revenue");
        accountsCreated += createTemplateAccount(organizationId, "4030", "Other Income", "REVENUE", "Non-operating Revenue", 2, false, createdBy, "Other income");
        
        // Cost of Goods Sold
        accountsCreated += createTemplateAccount(organizationId, "5000", "Cost of Goods Sold", "EXPENSE", "Cost of Sales", 1, true, createdBy, "Group for COGS accounts");
        accountsCreated += createTemplateAccount(organizationId, "5010", "Materials Cost", "EXPENSE", "Cost of Sales", 2, false, createdBy, "Materials cost");
        accountsCreated += createTemplateAccount(organizationId, "5020", "Direct Labor", "EXPENSE", "Cost of Sales", 2, false, createdBy, "Direct labor cost");
        accountsCreated += createTemplateAccount(organizationId, "5030", "Manufacturing Overhead", "EXPENSE", "Cost of Sales", 2, false, createdBy, "Manufacturing overhead");
        
        // Operating Expenses
        accountsCreated += createTemplateAccount(organizationId, "6000", "Operating Expenses", "EXPENSE", "Operating Expenses", 1, true, createdBy, "Group for operating expenses");
        accountsCreated += createTemplateAccount(organizationId, "6010", "Salaries and Wages", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Salaries and wages");
        accountsCreated += createTemplateAccount(organizationId, "6020", "Rent Expense", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Rent expense");
        accountsCreated += createTemplateAccount(organizationId, "6030", "Utilities", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Utilities expense");
        accountsCreated += createTemplateAccount(organizationId, "6040", "Insurance", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Insurance expense");
        accountsCreated += createTemplateAccount(organizationId, "6050", "Depreciation", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Depreciation expense");
        accountsCreated += createTemplateAccount(organizationId, "6060", "Office Supplies", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Office supplies");
        accountsCreated += createTemplateAccount(organizationId, "6070", "Marketing and Advertising", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Marketing and advertising");
        accountsCreated += createTemplateAccount(organizationId, "6080", "Professional Fees", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Professional fees");
        accountsCreated += createTemplateAccount(organizationId, "6090", "Travel and Entertainment", "EXPENSE", "Operating Expenses", 2, false, createdBy, "Travel and entertainment");
        
        log.info("Successfully created {} accounts for organization: {}", accountsCreated, organizationId);
        return accountsCreated;
    }
    
    private int createTemplateAccount(UUID organizationId, String code, String name, String type, 
                                     String category, int level, boolean isGroup, UUID createdBy, String description) {
        try {
            ChartOfAccounts account = new ChartOfAccounts();
            account.setOrganizationId(organizationId);
            account.setAccountCode(code);
            account.setAccountName(name);
            account.setAccountType(type);
            account.setAccountCategory(category);
            account.setLevel(level);
            account.setIsGroup(isGroup);
            account.setDescription(description);
            account.setCreatedBy(createdBy);
            account.setIsActive(true);
            account.setAllowManualEntry(!isGroup); // Group accounts don't allow manual entry
            account.setIsSystemAccount(true); // Mark standard accounts as system accounts (non-removable)
            
            coaRepository.save(account);
            return 1;
        } catch (Exception e) {
            log.error("Failed to create account {} - {}: {}", code, name, e.getMessage());
            return 0;
        }
    }
}

