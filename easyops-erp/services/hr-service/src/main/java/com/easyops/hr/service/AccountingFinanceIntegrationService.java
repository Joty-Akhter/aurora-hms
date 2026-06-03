package com.easyops.hr.service;

import com.easyops.hr.dto.PayrollAccountingExportDto;
import com.easyops.hr.entity.*;
import com.easyops.hr.integration.IntegrationCorrelationFilter;
import com.easyops.hr.integration.IntegrationCorrelationIdHolder;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration service for Accounting/Finance system
 * Handles integration of Provident Fund costs with accounting system
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountingFinanceIntegrationService {
    
    private final EpfContributionRepository epfContributionRepository;
    private final EpfAccountRepository epfAccountRepository;
    private final ProvidentFundRemittanceService providentFundRemittanceService;
    private final RestTemplate restTemplate;
    
    @Value("${services.accounting.base-url:http://accounting-service/api}")
    private String accountingServiceBaseUrl;

    @Value("${integration.accounting.system-user-id:00000000-0000-0000-0000-000000000001}")
    private String accountingSystemUserId;
    
    /**
     * Post EPF contributions to accounting system.
     * Uses Journal Integration API format. Requires EPF_PAYABLE and CASH in Chart of Accounts.
     */
    public Map<String, Object> postEpfContributionsToAccounting(UUID organizationId, Integer month, Integer year) {
        return postEpfContributionsToAccounting(organizationId, month, year, null);
    }

    public Map<String, Object> postEpfContributionsToAccounting(UUID organizationId, Integer month, Integer year, String actorUserId) {
        log.info("Posting EPF contributions to accounting system for organization: {}, period: {}/{}",
                organizationId, month, year);

        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationAndPeriod(organizationId, month, year);

        BigDecimal totalEmployeeContributions = contributions.stream()
                .map(c -> c.getEmployeeContributionAmount() != null ? c.getEmployeeContributionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEmployerContributions = contributions.stream()
                .map(c -> c.getEmployerContributionAmount() != null ? c.getEmployerContributionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = totalEmployeeContributions.add(totalEmployerContributions);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No EPF contributions to post for period " + month + "/" + year);
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(Map.of(
                "accountCode", "EPF_PAYABLE",
                "debitAmount", totalAmount,
                "creditAmount", BigDecimal.ZERO,
                "description", "EPF contributions " + month + "/" + year
        ));
        lines.add(Map.of(
                "accountCode", "CASH",
                "debitAmount", BigDecimal.ZERO,
                "creditAmount", totalAmount,
                "description", "EPF payment"
        ));

        String referenceId = "EPF-" + organizationId + "-" + month + "-" + year;
        String idempotencyKey = referenceId;
        String correlationId = IntegrationCorrelationIdHolder.get();

        Map<String, Object> journalEntry = new HashMap<>();
        journalEntry.put("organizationId", organizationId.toString());
        journalEntry.put("entryDate", LocalDate.of(year, month, 1).toString());
        journalEntry.put("journalType", "EPF_CONTRIBUTION");
        journalEntry.put("description", "EPF Contributions for " + month + "/" + year);
        journalEntry.put("referenceId", referenceId);
        journalEntry.put("status", "POSTED");
        journalEntry.put("lines", lines);
        if (correlationId != null && !correlationId.isBlank()) {
            journalEntry.put("correlationId", correlationId);
        }

        try {
            String url = accountingServiceBaseUrl + "/accounting/journal-entries";
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    url, jsonEntity(journalEntry, idempotencyKey, correlationId, actorUserId), Map.class);
            var remittance = providentFundRemittanceService.registerAccountingPosting(
                    organizationId, month, year, totalAmount, referenceId, actorUserId);
            log.info("Successfully posted EPF contributions to accounting system");
            Map<String, Object> out = enrichIntegrationMeta(result, correlationId, idempotencyKey);
            out.put("epfRemittanceId", remittance.getRemittanceId());
            out.put("epfRemittanceStatus", remittance.getStatus());
            return out;
        } catch (Exception e) {
            log.error("Error posting EPF contributions to accounting system", e);
            throw new RuntimeException("Failed to post EPF contributions to accounting system: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sync Provident Fund balances with accounting system
     */
    public void syncProvidentFundBalances(UUID organizationId) {
        log.info("Syncing Provident Fund balances with accounting system for organization: {}", organizationId);
        
        // Get total EPF liability
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        BigDecimal totalLiability = accounts.stream()
                .map(EpfAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> balanceSync = new HashMap<>();
        balanceSync.put("organizationId", organizationId);
        balanceSync.put("accountType", "EPF_LIABILITY");
        balanceSync.put("balance", totalLiability);
        balanceSync.put("syncDate", LocalDate.now());
        
        try {
            String url = accountingServiceBaseUrl + "/account-balances/sync";
            restTemplate.postForObject(url, balanceSync, Map.class);
            log.info("Successfully synced Provident Fund balances with accounting system");
        } catch (Exception e) {
            log.error("Error syncing Provident Fund balances with accounting system", e);
            throw new RuntimeException("Failed to sync Provident Fund balances", e);
        }
    }
    
    /**
     * Post payroll run to accounting system (create and post journal entry).
     * Uses **summary** account codes: 6110 (Salaries expense), 2020 (Accrued/deductions), 1030 (Bank).
     * For **component-level** journals using INT-20 GL codes, consume {@link PayrollAccountingExportDto#getDetailLines()}
     * (and per-component accounts) from the export API in accounting or a custom poster—this method does not expand detail lines.
     */
    public Map<String, Object> postPayrollToAccounting(PayrollAccountingExportDto export) {
        if (export == null || export.getOrganizationId() == null) {
            throw new IllegalArgumentException("Payroll export is required");
        }
        BigDecimal totalGross = export.getTotalGross() != null ? export.getTotalGross() : BigDecimal.ZERO;
        BigDecimal totalDeductions = export.getTotalDeductions() != null ? export.getTotalDeductions() : BigDecimal.ZERO;
        BigDecimal totalNet = export.getTotalNet() != null ? export.getTotalNet() : BigDecimal.ZERO;
        if (totalGross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payroll has no gross amount to post");
        }

        LocalDate entryDate = export.getPayPeriodEnd() != null ? export.getPayPeriodEnd() : LocalDate.now();
        String description = "Payroll for period " + export.getPayPeriodStart() + " to " + export.getPayPeriodEnd();
        String referenceId = export.getIdempotencyKey() != null && !export.getIdempotencyKey().isBlank()
                ? export.getIdempotencyKey()
                : "PAYROLL-" + export.getPayrollRunId();
        String correlationId = export.getCorrelationId();
        String idempotencyKey = referenceId;

        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(Map.of(
                "accountCode", "6110",
                "debitAmount", totalGross,
                "creditAmount", BigDecimal.ZERO,
                "description", "Gross salary expense"
        ));
        if (totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(Map.of(
                    "accountCode", "2020",
                    "debitAmount", BigDecimal.ZERO,
                    "creditAmount", totalDeductions,
                    "description", "Payroll deductions (accrued)"
            ));
        }
        lines.add(Map.of(
                "accountCode", "1030",
                "debitAmount", BigDecimal.ZERO,
                "creditAmount", totalNet,
                "description", "Net pay (bank)"
        ));

        Map<String, Object> journalEntry = new HashMap<>();
        journalEntry.put("organizationId", export.getOrganizationId().toString());
        journalEntry.put("entryDate", entryDate.toString());
        journalEntry.put("journalType", "PAYROLL");
        journalEntry.put("description", description);
        journalEntry.put("referenceId", referenceId);
        journalEntry.put("status", "POSTED");
        journalEntry.put("lines", lines);
        journalEntry.put("payrollRunId", export.getPayrollRunId().toString());
        if (correlationId != null && !correlationId.isBlank()) {
            journalEntry.put("correlationId", correlationId);
        }

        try {
            String url = accountingServiceBaseUrl + "/accounting/journal-entries";
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(
                    url, jsonEntity(journalEntry, idempotencyKey, correlationId, null), Map.class);
            log.info("Successfully posted payroll to accounting: run={}", export.getPayrollRunId());
            return enrichIntegrationMeta(result, correlationId, idempotencyKey);
        } catch (Exception e) {
            log.error("Error posting payroll to accounting", e);
            throw new RuntimeException("Failed to post payroll to accounting: " + e.getMessage(), e);
        }
    }

    /**
     * INT-41/INT-42: JSON body plus {@code Idempotency-Key} and {@code X-Correlation-Id} for downstream accounting.
     */
    private <T> HttpEntity<T> jsonEntity(T body, String idempotencyKey, String correlationId, String actorUserId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String userId = actorUserId != null && !actorUserId.isBlank() ? actorUserId : accountingSystemUserId;
        headers.set("X-User-Id", userId);
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set(IntegrationCorrelationFilter.HEADER_CORRELATION, correlationId);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return new HttpEntity<>(body, headers);
    }

    private static Map<String, Object> enrichIntegrationMeta(
            Map<String, Object> result, String correlationId, String idempotencyKey) {
        Map<String, Object> out = result != null ? new HashMap<>(result) : new HashMap<>();
        if (correlationId != null && !correlationId.isBlank()) {
            out.putIfAbsent("correlationId", correlationId);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            out.putIfAbsent("idempotencyKey", idempotencyKey);
        }
        return out;
    }

    /**
     * Get accounting period status
     */
    public Map<String, Object> getAccountingPeriodStatus(UUID organizationId, Integer month, Integer year) {
        log.info("Getting accounting period status for organization: {}, period: {}/{}", 
                organizationId, month, year);
        
        try {
            String url = accountingServiceBaseUrl + "/periods/status?organizationId=" + organizationId +
                    "&month=" + month + "&year=" + year;
            @SuppressWarnings("unchecked")
            Map<String, Object> status = restTemplate.getForObject(url, Map.class);
            return status != null ? status : new HashMap<>();
        } catch (Exception e) {
            log.error("Error getting accounting period status", e);
            return new HashMap<>();
        }
    }
}

