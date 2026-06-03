package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for territory incentive rule with allocations and validation status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryIncentiveRuleResponse {

    private UUID id;
    private UUID organizationId;
    private UUID territoryId;

    private BigDecimal incentivePercentage;
    private BigDecimal srSharePercentage;
    private BigDecimal developmentFundPercentage;
    private Boolean hasDedicatedSr;
    private UUID dualRoleEmployeeId;

    private BigDecimal expenseLimitPercentage;
    private Integer ruleVersion;
    private LocalDate effectiveFromDate;
    private LocalDate effectiveToDate;
    private Boolean isActive;
    private String description;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AllocationItem> allocations = new ArrayList<>();
    private String validationStatus = "VALID"; // VALID, or error message when validation fails

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationItem {
        private UUID id;
        private UUID employeeId;
        private String roleInTerritory;
        private BigDecimal allocationPercentage;
    }
}
