package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class FinancialReportDTO {
    
    // Accounts Balance Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountsBalance {
        private LocalDate asOfDate;
        private List<AreaBalance> areaBalances;
        private BigDecimal totalDueAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaBalance {
        private UUID areaId;
        private String areaName;
        private BigDecimal totalProductsSupplied;
        private BigDecimal totalDepositsReceived;
        private BigDecimal dueAmount;
        private BigDecimal overdueAmount;
    }
    
    // Income and Expense Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeExpense {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalIncome; // Total deposits/collections
        private BigDecimal totalExpenses;
        private BigDecimal netIncome;
        private List<AreaIncomeExpense> areaDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaIncomeExpense {
        private UUID areaId;
        private String areaName;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal netIncome;
    }
    
    // Incentive Report
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncentiveReport {
        private Integer year;
        private Integer month;
        private List<AreaIncentiveDetail> areaIncentives;
        private BigDecimal totalIncentiveAmount;
        private Integer eligibleAreas;
        private Integer totalAreas;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaIncentiveDetail {
        private UUID areaId;
        private String areaName;
        private BigDecimal targetAmount;
        private BigDecimal coveredAmount;
        private BigDecimal targetCoveragePercentage;
        private Boolean targetAchieved;
        private Boolean expenseWithinLimit;
        private Boolean eligible;
        private BigDecimal incentiveBaseAmount;
        private List<EmployeeIncentiveDetail> employeeIncentives;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeIncentiveDetail {
        private UUID employeeId;
        private String employeeName;
        private String employeeIdCode;
        private String role;
        private BigDecimal incentiveAmount;
    }
}

