package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyClosingReportDTO {
    
    // Header Information
    private Integer year;
    private Integer month;
    private String divisionName;
    private String regionName;
    private String territoryName;
    private String areaName;
    private String employeeName;
    private String employeeId;
    private String designation;
    
    // Product-Wise Details
    private List<ProductWiseDetail> productDetails;
    
    // Financial Summary
    private BigDecimal totalProductsSuppliedValue;
    private BigDecimal totalDepositsReceived;
    private BigDecimal dueAmount;
    private BigDecimal targetAmount;
    private BigDecimal coveredAmount;
    private BigDecimal targetCoveragePercentage;
    
    // Performance Metrics
    private Boolean targetAchieved;
    private BigDecimal collectionEfficiency;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductWiseDetail {
        private UUID productId;
        private String productName;
        private BigDecimal openingBalance;
        private BigDecimal quantityReceived;
        private BigDecimal quantitySold;
        private BigDecimal quantityAdjusted;
        private BigDecimal closingBalance;
        private BigDecimal tradePricePerUnit;
        private BigDecimal totalValue;
        private List<TransactionDetail> transactions;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetail {
        private LocalDate transactionDate;
        private String transactionType; // ALLOCATION, SALE, ADJUSTMENT
        private BigDecimal quantity;
        private String reason; // For adjustments
    }
}

