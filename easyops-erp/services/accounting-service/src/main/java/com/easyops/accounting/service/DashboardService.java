package com.easyops.accounting.service;

import com.easyops.accounting.dto.DashboardSummaryResponse;
import com.easyops.accounting.entity.JournalEntry;
import com.easyops.accounting.repository.JournalEntryRepository;
import com.easyops.accounting.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    
    @Value("${services.ar.url:http://localhost:8090}")
    private String arServiceUrl;
    
    @Value("${services.ap.url:http://localhost:8091}")
    private String apServiceUrl;
    
    @Value("${services.bank.url:http://localhost:8092}")
    private String bankServiceUrl;
    
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(UUID organizationId, UUID actorUserId) {
        log.info("Generating dashboard summary for organization: {}", organizationId);
        
        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        
        try {
            // Get AR Summary
            getARSummary(organizationId, actorUserId, summary);
        } catch (Exception e) {
            log.error("Failed to get AR summary", e);
        }
        
        try {
            // Get AP Summary
            getAPSummary(organizationId, actorUserId, summary);
        } catch (Exception e) {
            log.error("Failed to get AP summary", e);
        }
        
        try {
            // Get Bank Summary
            getBankSummary(organizationId, actorUserId, summary);
        } catch (Exception e) {
            log.error("Failed to get bank summary", e);
        }
        
        try {
            // Get Recent Transactions
            getRecentTransactions(organizationId, summary);
        } catch (Exception e) {
            log.error("Failed to get recent transactions", e);
        }
        
        try {
            // Generate Alerts
            generateAlerts(summary);
        } catch (Exception e) {
            log.error("Failed to generate alerts", e);
        }
        
        return summary;
    }
    
    private void getARSummary(UUID organizationId, UUID actorUserId, DashboardSummaryResponse summary) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(userHeaders(actorUserId));
            // Get outstanding invoices
            String url = arServiceUrl + "/api/ar/invoices/outstanding?organizationId=" + organizationId;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> invoices = response.getBody();
            if (invoices != null) {
                summary.setTotalInvoices(invoices.size());
                BigDecimal total = BigDecimal.ZERO;
                for (Map<String, Object> inv : invoices) {
                    Object balanceDue = inv.get("balanceDue");
                    if (balanceDue != null) {
                        total = total.add(new BigDecimal(balanceDue.toString()));
                    }
                }
                summary.setTotalReceivables(total);
            }
            
            // Get overdue invoices
            String overdueUrl = arServiceUrl + "/api/ar/invoices/overdue?organizationId=" + organizationId;
            ResponseEntity<List<Map<String, Object>>> overdueResponse = restTemplate.exchange(
                    overdueUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> overdueInvoices = overdueResponse.getBody();
            if (overdueInvoices != null) {
                summary.setOverdueInvoices(overdueInvoices.size());
                BigDecimal overdueTotal = BigDecimal.ZERO;
                for (Map<String, Object> inv : overdueInvoices) {
                    Object balanceDue = inv.get("balanceDue");
                    if (balanceDue != null) {
                        overdueTotal = overdueTotal.add(new BigDecimal(balanceDue.toString()));
                    }
                }
                summary.setOverdueReceivables(overdueTotal);
            }
            
            // Calculate current (not overdue)
            BigDecimal current = summary.getTotalReceivables().subtract(summary.getOverdueReceivables());
            summary.setCurrentReceivables(current);
            
        } catch (Exception e) {
            log.error("Error getting AR summary", e);
            summary.setTotalReceivables(BigDecimal.ZERO);
            summary.setOverdueReceivables(BigDecimal.ZERO);
            summary.setCurrentReceivables(BigDecimal.ZERO);
        }
    }
    
    private void getAPSummary(UUID organizationId, UUID actorUserId, DashboardSummaryResponse summary) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(userHeaders(actorUserId));
            // Get outstanding bills
            String url = apServiceUrl + "/api/ap/bills/outstanding?organizationId=" + organizationId;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> bills = response.getBody();
            if (bills != null) {
                summary.setTotalBills(bills.size());
                BigDecimal total = BigDecimal.ZERO;
                for (Map<String, Object> bill : bills) {
                    Object balanceDue = bill.get("balanceDue");
                    if (balanceDue != null) {
                        total = total.add(new BigDecimal(balanceDue.toString()));
                    }
                }
                summary.setTotalPayables(total);
            }
            
            // Get overdue bills
            String overdueUrl = apServiceUrl + "/api/ap/bills/overdue?organizationId=" + organizationId;
            ResponseEntity<List<Map<String, Object>>> overdueResponse = restTemplate.exchange(
                    overdueUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> overdueBills = overdueResponse.getBody();
            if (overdueBills != null) {
                summary.setOverdueBills(overdueBills.size());
                BigDecimal overdueTotal = BigDecimal.ZERO;
                for (Map<String, Object> bill : overdueBills) {
                    Object balanceDue = bill.get("balanceDue");
                    if (balanceDue != null) {
                        overdueTotal = overdueTotal.add(new BigDecimal(balanceDue.toString()));
                    }
                }
                summary.setOverduePayables(overdueTotal);
            }
            
            // Calculate current
            BigDecimal current = summary.getTotalPayables().subtract(summary.getOverduePayables());
            summary.setCurrentPayables(current);
            
        } catch (Exception e) {
            log.error("Error getting AP summary", e);
            summary.setTotalPayables(BigDecimal.ZERO);
            summary.setOverduePayables(BigDecimal.ZERO);
            summary.setCurrentPayables(BigDecimal.ZERO);
        }
    }
    
    private void getBankSummary(UUID organizationId, UUID actorUserId, DashboardSummaryResponse summary) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(userHeaders(actorUserId));
            String url = bankServiceUrl + "/api/bank/accounts?organizationId=" + organizationId;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> accounts = response.getBody();
            if (accounts != null) {
                BigDecimal totalBalance = BigDecimal.ZERO;
                List<DashboardSummaryResponse.BankAccountSummary> bankSummaries = new ArrayList<>();
                
                for (Map<String, Object> acc : accounts) {
                    Object currentBalance = acc.get("currentBalance");
                    BigDecimal balance = currentBalance != null ? new BigDecimal(currentBalance.toString()) : BigDecimal.ZERO;
                    totalBalance = totalBalance.add(balance);
                    
                    DashboardSummaryResponse.BankAccountSummary bankSummary = new DashboardSummaryResponse.BankAccountSummary();
                    bankSummary.setAccountName(acc.get("accountName") != null ? acc.get("accountName").toString() : "");
                    bankSummary.setAccountNumber(acc.get("accountNumber") != null ? acc.get("accountNumber").toString() : "");
                    bankSummary.setBalance(balance);
                    bankSummary.setAccountType(acc.get("accountType") != null ? acc.get("accountType").toString() : "");
                    bankSummaries.add(bankSummary);
                }
                
                summary.setTotalBankBalance(totalBalance);
                summary.setTotalCash(totalBalance); // For now, same as bank balance
                summary.setBankAccounts(bankSummaries);
            }
        } catch (Exception e) {
            log.error("Error getting bank summary", e);
            summary.setTotalBankBalance(BigDecimal.ZERO);
            summary.setTotalCash(BigDecimal.ZERO);
        }
    }
    
    private void getRecentTransactions(UUID organizationId, DashboardSummaryResponse summary) {
        try {
            List<JournalEntry> entries = journalEntryRepository
                    .findTop10ByOrganizationIdAndStatusOrderByPostedAtDesc(organizationId, "POSTED");
            Map<UUID, BigDecimal> lineDebitTotalsByEntryId = loadLineDebitTotals(entries);

            List<DashboardSummaryResponse.RecentTransaction> transactions = new ArrayList<>();
            for (JournalEntry entry : entries) {
                DashboardSummaryResponse.RecentTransaction txn = new DashboardSummaryResponse.RecentTransaction();
                txn.setDate(entry.getJournalDate().toString());
                txn.setType(entry.getJournalType() != null ? entry.getJournalType() : "JOURNAL");
                txn.setReference(entry.getJournalNumber() != null ? entry.getJournalNumber() : entry.getReferenceNumber());
                txn.setDescription(entry.getDescription() != null ? entry.getDescription() : "Journal Entry");
                txn.setAmount(resolveTransactionAmount(entry, lineDebitTotalsByEntryId));
                transactions.add(txn);
            }

            summary.setRecentTransactions(transactions);
        } catch (Exception e) {
            log.error("Error getting recent transactions", e);
        }
    }

    private Map<UUID, BigDecimal> loadLineDebitTotals(List<JournalEntry> entries) {
        List<UUID> entryIdsNeedingLineSum = entries.stream()
                .filter(this::needsLineDebitFallback)
                .map(JournalEntry::getId)
                .toList();
        if (entryIdsNeedingLineSum.isEmpty()) {
            return Map.of();
        }
        return journalLineRepository.sumDebitAmountsGroupedByJournalEntryId(entryIdsNeedingLineSum).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1],
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private boolean needsLineDebitFallback(JournalEntry entry) {
        BigDecimal totalDebit = entry.getTotalDebit();
        return totalDebit == null || totalDebit.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Prefer persisted header totals (maintained by DB trigger); fall back to batched line sums for legacy rows.
     */
    BigDecimal resolveTransactionAmount(JournalEntry entry, Map<UUID, BigDecimal> lineDebitTotalsByEntryId) {
        BigDecimal totalDebit = entry.getTotalDebit();
        if (totalDebit != null && totalDebit.compareTo(BigDecimal.ZERO) > 0) {
            return totalDebit;
        }
        return lineDebitTotalsByEntryId.getOrDefault(entry.getId(), BigDecimal.ZERO);
    }
    
    private void generateAlerts(DashboardSummaryResponse summary) {
        List<DashboardSummaryResponse.DashboardAlert> alerts = new ArrayList<>();
        
        // Alert for overdue receivables
        if (summary.getOverdueReceivables() != null && summary.getOverdueReceivables().compareTo(BigDecimal.ZERO) > 0) {
            DashboardSummaryResponse.DashboardAlert alert = new DashboardSummaryResponse.DashboardAlert();
            alert.setType("OVERDUE_RECEIVABLES");
            alert.setSeverity("WARNING");
            alert.setMessage(String.format("You have %d overdue invoices totaling $%.2f", 
                    summary.getOverdueInvoices(), summary.getOverdueReceivables()));
            alert.setActionUrl("/accounting/invoices");
            alerts.add(alert);
        }
        
        // Alert for overdue payables
        if (summary.getOverduePayables() != null && summary.getOverduePayables().compareTo(BigDecimal.ZERO) > 0) {
            DashboardSummaryResponse.DashboardAlert alert = new DashboardSummaryResponse.DashboardAlert();
            alert.setType("OVERDUE_PAYABLES");
            alert.setSeverity("ERROR");
            alert.setMessage(String.format("You have %d overdue bills totaling $%.2f", 
                    summary.getOverdueBills(), summary.getOverduePayables()));
            alert.setActionUrl("/accounting/bills");
            alerts.add(alert);
        }
        
        // Alert for customers over credit limit
        if (summary.getCustomersOverCreditLimit() != null && summary.getCustomersOverCreditLimit() > 0) {
            DashboardSummaryResponse.DashboardAlert alert = new DashboardSummaryResponse.DashboardAlert();
            alert.setType("CREDIT_LIMIT");
            alert.setSeverity("WARNING");
            alert.setMessage(String.format("%d customers have exceeded their credit limits", 
                    summary.getCustomersOverCreditLimit()));
            alert.setActionUrl("/accounting/invoices");
            alerts.add(alert);
        }
        
        // Alert for low cash
        if (summary.getTotalCash() != null && summary.getTotalCash().compareTo(BigDecimal.valueOf(10000)) < 0) {
            DashboardSummaryResponse.DashboardAlert alert = new DashboardSummaryResponse.DashboardAlert();
            alert.setType("LOW_CASH");
            alert.setSeverity("WARNING");
            alert.setMessage(String.format("Cash balance is low: $%.2f", summary.getTotalCash()));
            alert.setActionUrl("/accounting/bank-accounts");
            alerts.add(alert);
        }
        
        summary.setAlerts(alerts);
    }

    private static HttpHeaders userHeaders(UUID actorUserId) {
        HttpHeaders headers = new HttpHeaders();
        if (actorUserId != null) {
            headers.set("X-User-Id", actorUserId.toString());
        }
        return headers;
    }
}

