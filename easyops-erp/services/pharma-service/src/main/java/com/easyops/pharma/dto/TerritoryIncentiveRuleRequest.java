package com.easyops.pharma.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating/updating territory incentive rule with allocations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryIncentiveRuleRequest {

    private UUID id;
    @NotNull
    private UUID organizationId;
    @NotNull
    private UUID territoryId;

    private BigDecimal incentivePercentage = new BigDecimal("0.0400");
    private BigDecimal srSharePercentage = new BigDecimal("0.0900");
    private BigDecimal developmentFundPercentage = new BigDecimal("0.0100");
    private Boolean hasDedicatedSr = true;
    private UUID dualRoleEmployeeId;

    private BigDecimal expenseLimitPercentage = new BigDecimal("0.3000");
    private LocalDate effectiveFromDate;
    private LocalDate effectiveToDate;
    private Boolean isActive = true;
    private String description;
    private String notes;

    @Valid
    private List<AllocationItem> allocations = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationItem {
        @NotNull
        private UUID employeeId;
        private String roleInTerritory;
        @NotNull
        private BigDecimal allocationPercentage;
    }
}
