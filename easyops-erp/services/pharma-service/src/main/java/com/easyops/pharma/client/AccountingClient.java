package com.easyops.pharma.client;

import com.easyops.pharma.config.AccountingFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Accounting Service
 * Used for posting financial transactions from pharma operations
 */
@FeignClient(name = "accounting-service", configuration = AccountingFeignConfig.class)
public interface AccountingClient {
    
    /**
     * Create a journal entry for deposit transaction
     */
    @PostMapping("/api/accounting/journal-entries")
    Map<String, Object> createJournalEntry(@RequestBody Map<String, Object> journalEntryRequest);
    
    /**
     * Create a journal entry for incentive payment
     */
    @PostMapping("/api/accounting/journal-entries/incentive")
    Map<String, Object> createIncentiveJournalEntry(@RequestBody Map<String, Object> incentiveEntryRequest);
    
    /**
     * Get journal entry by ID
     */
    @GetMapping("/api/accounting/journal-entries/{id}")
    Map<String, Object> getJournalEntry(@PathVariable("id") UUID id);
}
