package com.easyops.pharma.service;

import com.easyops.pharma.client.AccountingClient;
import com.easyops.pharma.entity.Deposit;
import com.easyops.pharma.entity.IncentiveCalculation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for integrating with Accounting Service
 * Handles automatic journal entry creation for deposits and incentives
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingIntegrationService {
    
    private final AccountingClient accountingClient;
    
    /**
     * Post journal entry for deposit transaction
     * Creates AR entry when deposit is completed
     */
    @Transactional
    public void postDepositJournalEntry(Deposit deposit) {
        try {
            log.info("Posting deposit journal entry for deposit: {}", deposit.getId());
            
            Map<String, Object> journalEntry = new HashMap<>();
            journalEntry.put("organizationId", deposit.getOrganizationId().toString());
            journalEntry.put("entryDate", deposit.getDepositDate().toString());
            journalEntry.put("journalType", "PHARMA_DEPOSIT");
            journalEntry.put("referenceId", deposit.getId().toString());
            journalEntry.put("description", "Pharma Deposit - Territory: " + deposit.getTerritoryId());
            journalEntry.put("sourceModule", "PHARMA");
            journalEntry.put("sourceDocumentId", deposit.getId().toString());
            
            // Journal entry lines
            java.util.List<Map<String, Object>> lines = new java.util.ArrayList<>();
            
            // Debit: Bank/Cash Account (increase asset)
            Map<String, Object> debitLine = new HashMap<>();
            debitLine.put("accountCode", "CASH"); // Will need to be configured per organization
            debitLine.put("debitAmount", deposit.getDepositAmount());
            debitLine.put("creditAmount", BigDecimal.ZERO);
            debitLine.put("description", "Deposit from Territory: " + deposit.getTerritoryId());
            lines.add(debitLine);
            
            // Credit: Accounts Receivable (decrease asset)
            Map<String, Object> creditLine = new HashMap<>();
            creditLine.put("accountCode", "AR_PHARMA"); // Accounts Receivable - Pharma
            creditLine.put("debitAmount", BigDecimal.ZERO);
            creditLine.put("creditAmount", deposit.getDepositAmount());
            creditLine.put("description", "Collection from Territory: " + deposit.getTerritoryId());
            lines.add(creditLine);
            
            journalEntry.put("lines", lines);
            journalEntry.put("status", "POSTED");
            
            Map<String, Object> result = accountingClient.createJournalEntry(journalEntry);
            log.info("Successfully posted deposit journal entry. Entry ID: {}", result.get("id"));
            
        } catch (Exception e) {
            log.error("Failed to post deposit journal entry for deposit: {}", deposit.getId(), e);
            // Don't throw exception - log error but don't block deposit creation
        }
    }
    
    /**
     * Post journal entry for incentive calculation
     * Creates expense entry when incentives are calculated
     */
    @Transactional
    public void postIncentiveJournalEntry(IncentiveCalculation calculation) {
        try {
            if (!Boolean.TRUE.equals(calculation.getTerritoryEligible()) || calculation.getIncentiveBaseAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping incentive journal entry - territory not eligible or zero amount");
                return;
            }
            
            log.info("Posting incentive journal entry for calculation: {}", calculation.getId());
            
            Map<String, Object> journalEntry = new HashMap<>();
            journalEntry.put("organizationId", calculation.getOrganizationId().toString());
            journalEntry.put("entryDate", calculation.getCalculationDate().toLocalDate().toString());
            journalEntry.put("journalType", "PHARMA_INCENTIVE");
            journalEntry.put("referenceId", calculation.getId().toString());
            journalEntry.put("description", "Pharma Incentive - Territory: " + calculation.getTerritoryId());
            journalEntry.put("sourceModule", "PHARMA");
            journalEntry.put("sourceDocumentId", calculation.getId().toString());
            
            // Journal entry lines
            java.util.List<Map<String, Object>> lines = new java.util.ArrayList<>();
            
            // Debit: Incentive Expense Account
            Map<String, Object> debitLine = new HashMap<>();
            debitLine.put("accountCode", "INCENTIVE_EXPENSE"); // Incentive expense account
            debitLine.put("debitAmount", calculation.getIncentiveBaseAmount());
            debitLine.put("creditAmount", BigDecimal.ZERO);
            debitLine.put("description", "Incentive for Territory: " + calculation.getTerritoryId());
            lines.add(debitLine);
            
            // Credit: Incentive Payable Account (liability)
            Map<String, Object> creditLine = new HashMap<>();
            creditLine.put("accountCode", "INCENTIVE_PAYABLE"); // Incentive payable account
            creditLine.put("debitAmount", BigDecimal.ZERO);
            creditLine.put("creditAmount", calculation.getIncentiveBaseAmount());
            creditLine.put("description", "Incentive Payable - Territory: " + calculation.getTerritoryId());
            lines.add(creditLine);
            
            journalEntry.put("lines", lines);
            journalEntry.put("status", "POSTED");
            
            Map<String, Object> result = accountingClient.createIncentiveJournalEntry(journalEntry);
            log.info("Successfully posted incentive journal entry. Entry ID: {}", result.get("id"));
            
        } catch (Exception e) {
            log.error("Failed to post incentive journal entry for calculation: {}", calculation.getId(), e);
            // Don't throw exception - log error but don't block incentive calculation
        }
    }
}
