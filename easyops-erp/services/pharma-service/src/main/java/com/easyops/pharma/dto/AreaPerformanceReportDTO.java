package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaPerformanceReportDTO {
    
    private UUID areaId;
    private String areaName;
    private String divisionName;
    private String regionName;
    private String territoryName;
    
    // Target and Coverage
    private BigDecimal targetAmount;
    private BigDecimal coveredAmount;
    private BigDecimal targetCoveragePercentage;
    
    // Expenses
    private BigDecimal totalExpenses;
    private BigDecimal expensePercentage; // Expenses as % of target
    private Boolean expenseWithinLimit; // True if expenses <= 30% of target
    
    // Incentive Eligibility
    private Boolean incentiveEligible;
    private BigDecimal incentiveBaseAmount;
    
    // Employee Performance
    private List<EmployeePerformance> employeePerformances;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeePerformance {
        private UUID employeeId;
        private String employeeName;
        private String employeeIdCode;
        private String role;
        private BigDecimal incentiveAmount;
        private BigDecimal targetContribution;
        private BigDecimal coverageContribution;
    }
}

