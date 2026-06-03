package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InventoryReportDTO {
    
    // In-Stock Total Amount Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InStockTotalAmount {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate asOfDate;
        private String location;
        private BigDecimal totalInStockAmount;
    }
    
    // In-Stock Product-Wise Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InStockProductWise {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate asOfDate;
        private String location;
        private List<ProductStockDetail> products;
        private BigDecimal totalInStockAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockDetail {
        private UUID productId;
        private String productName;
        private BigDecimal packSize;
        private BigDecimal tpWithVat;
        private BigDecimal quantity;
        private BigDecimal amount;
    }
    
    // Area-Wise Allocation Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaWiseAllocation {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<AreaAllocationDetail> areas;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaAllocationDetail {
        private UUID areaId;
        private String areaName;
        private String receivingEmployeeName;
        private Integer totalAllocations;
        private List<ProductAllocationDetail> productDetails;
        private BigDecimal totalAllocationAmount;
        private List<LocalDate> allocationDates;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAllocationDetail {
        private UUID productId;
        private String productName;
        private BigDecimal totalQuantityAllocated;
        private BigDecimal totalAmount;
    }
    
    // Month-Wise / Annual Allocation Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthWiseAllocation {
        private Integer year;
        private List<MonthlyAllocationDetail> monthlyDetails;
        private BigDecimal grandTotalQuantity;
        private BigDecimal grandTotalAmount;
        private Integer grandTotalAllocations;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyAllocationDetail {
        private Integer year;
        private Integer month;
        private BigDecimal totalQuantity;
        private BigDecimal totalAmount;
        private Integer numberOfAllocations;
        private Integer numberOfAreas;
        private List<String> topProducts;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnualAllocation {
        private Integer year;
        private Integer totalAllocations;
        private BigDecimal totalQuantity;
        private BigDecimal totalAmount;
        private BigDecimal averageMonthlyAllocation;
        private String peakMonth;
        private List<MonthlyAllocationDetail> monthlyBreakdown;
    }
}

