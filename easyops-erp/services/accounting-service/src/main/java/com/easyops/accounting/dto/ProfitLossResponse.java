package com.easyops.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossResponse {
    private String organizationName;
    private String periodName;
    private String startDate;
    private String endDate;
    
    // Revenue section
    private List<AccountLineItem> revenueAccounts;
    private BigDecimal totalRevenue;
    
    // Cost of Goods Sold section
    private List<AccountLineItem> cogsAccounts;
    private BigDecimal totalCOGS;
    private BigDecimal grossProfit;
    private BigDecimal grossProfitMargin;
    
    // Operating Expenses section
    private List<AccountLineItem> operatingExpenses;
    private BigDecimal totalOperatingExpenses;
    
    // Net Income
    private BigDecimal operatingIncome;
    private BigDecimal netIncome;
    private BigDecimal netProfitMargin;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountLineItem {
        private String accountCode;
        private String accountName;
        private BigDecimal amount;
        private String accountCategory;
    }
}

