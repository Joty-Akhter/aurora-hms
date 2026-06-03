package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CollectionReportDTO {
    
    // Area-Wise Collection Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaWiseCollection {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<AreaCollectionDetail> areas;
        private BigDecimal grandTotalCollection;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaCollectionDetail {
        private UUID areaId;
        private String areaName;
        private Integer numberOfDeposits;
        private BigDecimal totalCollectionAmount;
        private BigDecimal targetAmount;
        private BigDecimal coveragePercentage;
        private List<DepositDetail> deposits;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositDetail {
        private UUID depositId;
        private LocalDate depositDate;
        private BigDecimal depositAmount;
        private String status;
        private String collectedBy;
    }
    
    // Employee-Wise Collection Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeWiseCollection {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<EmployeeCollectionDetail> employees;
        private BigDecimal grandTotalCollection;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeCollectionDetail {
        private UUID employeeId;
        private String employeeName;
        private String employeeIdCode;
        private String role;
        private Integer numberOfDeposits;
        private BigDecimal totalCollectionAmount;
        private List<UUID> assignedAreaIds;
        private List<String> assignedAreaNames;
    }
}

