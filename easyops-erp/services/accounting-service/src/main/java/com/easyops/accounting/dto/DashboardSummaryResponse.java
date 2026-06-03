package com.easyops.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    
    // AR Summary
    private BigDecimal totalReceivables;
    private BigDecimal currentReceivables;
    private BigDecimal overdueReceivables;
    private Integer totalInvoices;
    private Integer overdueInvoices;
    private Integer customersOverCreditLimit;
    
    // AP Summary
    private BigDecimal totalPayables;
    private BigDecimal currentPayables;
    private BigDecimal overduePayables;
    private Integer totalBills;
    private Integer overdueBills;
    private Integer billsDueThisWeek;
    
    // Cash Position
    private BigDecimal totalCash;
    private BigDecimal totalBankBalance;
    private List<BankAccountSummary> bankAccounts = new ArrayList<>();
    
    // GL Summary
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal netIncome;
    
    // Recent Activity
    private List<RecentTransaction> recentTransactions = new ArrayList<>();
    
    // Alerts
    private List<DashboardAlert> alerts = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankAccountSummary {
        private String accountName;
        private String accountNumber;
        private BigDecimal balance;
        private String accountType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransaction {
        private String date;
        private String type;
        private String reference;
        private String description;
        private BigDecimal amount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAlert {
        private String type;  // OVERDUE_INVOICE, CREDIT_LIMIT, LOW_CASH, etc.
        private String severity; // INFO, WARNING, ERROR
        private String message;
        private String actionUrl;
    }
}

