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
public class CashFlowResponse {
    private String organizationName;
    private String periodName;
    private String startDate;
    private String endDate;
    
    // Operating Activities
    private BigDecimal netIncome;
    private List<CashFlowLineItem> operatingAdjustments;
    private BigDecimal netCashFromOperations;
    
    // Investing Activities
    private List<CashFlowLineItem> investingActivities;
    private BigDecimal netCashFromInvesting;
    
    // Financing Activities
    private List<CashFlowLineItem> financingActivities;
    private BigDecimal netCashFromFinancing;
    
    // Summary
    private BigDecimal netCashFlow;
    private BigDecimal cashAtBeginning;
    private BigDecimal cashAtEnd;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowLineItem {
        private String description;
        private BigDecimal amount;
        private String accountCode;
    }
}

