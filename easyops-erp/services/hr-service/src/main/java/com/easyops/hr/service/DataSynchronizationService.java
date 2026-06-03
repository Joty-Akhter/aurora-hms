package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for data synchronization and validation
 * Handles periodic synchronization of data between HR service and external systems
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataSynchronizationService {
    
    private final EmployeeManagementIntegrationService employeeManagementIntegrationService;
    private final AccountingFinanceIntegrationService accountingFinanceIntegrationService;
    private final EpfAccountRepository epfAccountRepository;
    
    /**
     * Synchronize employee data (runs daily)
     */
    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM daily
    public void synchronizeEmployeeData() {
        log.info("Starting daily employee data synchronization");
        
        // Get all unique organizations
        List<UUID> organizationIds = epfAccountRepository.findAll().stream()
                .map(EpfAccount::getOrganizationId)
                .distinct()
                .toList();
        
        for (UUID organizationId : organizationIds) {
            try {
                employeeManagementIntegrationService.syncEmployeeDataForProvidentFund(organizationId);
            } catch (Exception e) {
                log.error("Error syncing employee data for organization: {}", organizationId, e);
            }
        }
        
        log.info("Completed employee data synchronization");
    }
    
    /**
     * Synchronize accounting data (runs monthly)
     */
    @Scheduled(cron = "0 0 4 1 * *") // Run at 4 AM on 1st of every month
    public void synchronizeAccountingData() {
        log.info("Starting monthly accounting data synchronization");
        
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        Integer month = lastMonth.getMonthValue();
        Integer year = lastMonth.getYear();
        
        // Get all unique organizations
        List<UUID> organizationIds = epfAccountRepository.findAll().stream()
                .map(EpfAccount::getOrganizationId)
                .distinct()
                .toList();
        
        for (UUID organizationId : organizationIds) {
            try {
                // Sync EPF contributions
                accountingFinanceIntegrationService.postEpfContributionsToAccounting(
                        organizationId, month, year);
                
                // Sync balances
                accountingFinanceIntegrationService.syncProvidentFundBalances(organizationId);
            } catch (Exception e) {
                log.error("Error syncing accounting data for organization: {}", organizationId, e);
            }
        }
        
        log.info("Completed accounting data synchronization");
    }
    
    /**
     * Validate data integrity
     */
    public Map<String, Object> validateDataIntegrity(UUID organizationId) {
        log.info("Validating data integrity for organization: {}", organizationId);
        
        Map<String, Object> validation = new HashMap<>();
        int errors = 0;
        List<String> errorMessages = new java.util.ArrayList<>();
        
        // Validate EPF accounts
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        for (EpfAccount account : accounts) {
            if (account.getCurrentBalance().compareTo(java.math.BigDecimal.ZERO) < 0) {
                errors++;
                errorMessages.add("EPF account " + account.getEpfAccountNumber() + 
                        " has negative balance");
            }
        }
        
        // Placeholder for additional validation logic
        
        validation.put("organizationId", organizationId);
        validation.put("totalErrors", errors);
        validation.put("errors", errorMessages);
        validation.put("status", errors == 0 ? "VALID" : "INVALID");
        validation.put("validatedAt", LocalDate.now());
        
        return validation;
    }
    
    /**
     * Manual synchronization trigger
     */
    public void triggerSynchronization(String syncType, UUID organizationId) {
        log.info("Triggering manual synchronization: {} for organization: {}", syncType, organizationId);
        
        switch (syncType.toLowerCase()) {
            case "employee":
                employeeManagementIntegrationService.syncEmployeeDataForProvidentFund(organizationId);
                break;
            case "accounting":
                LocalDate lastMonth = LocalDate.now().minusMonths(1);
                accountingFinanceIntegrationService.postEpfContributionsToAccounting(
                        organizationId, lastMonth.getMonthValue(), lastMonth.getYear());
                break;
            default:
                throw new RuntimeException("Unknown synchronization type: " + syncType);
        }
    }
}

