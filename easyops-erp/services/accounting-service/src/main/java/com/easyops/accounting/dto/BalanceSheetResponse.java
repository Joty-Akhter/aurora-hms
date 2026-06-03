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
public class BalanceSheetResponse {
    private String organizationName;
    private String asOfDate;
    
    // Assets section
    private List<AccountLineItem> currentAssets;
    private BigDecimal totalCurrentAssets;
    private List<AccountLineItem> fixedAssets;
    private BigDecimal totalFixedAssets;
    private BigDecimal totalAssets;
    
    // Liabilities section
    private List<AccountLineItem> currentLiabilities;
    private BigDecimal totalCurrentLiabilities;
    private List<AccountLineItem> longTermLiabilities;
    private BigDecimal totalLongTermLiabilities;
    private BigDecimal totalLiabilities;
    
    // Equity section
    private List<AccountLineItem> equityAccounts;
    private BigDecimal totalEquity;
    
    // Balance check
    private BigDecimal totalLiabilitiesAndEquity;
    private boolean balanced;
    
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

