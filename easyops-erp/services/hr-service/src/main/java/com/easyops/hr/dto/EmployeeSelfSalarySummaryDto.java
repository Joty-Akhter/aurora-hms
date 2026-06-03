package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * ES-53: Employee self-service – view own current salary (structure name, grade name, component names).
 * Amounts may be masked per organization policy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSelfSalarySummaryDto {
    private String structureCode;
    private String structureName;
    private String gradeCode;
    private String gradeName;
    private String bandCode;
    private String bandName;
    private String currency;
    private String payFrequency;
    /** Component lines: name/code and optionally amount (masked as null or "***" per policy). */
    private List<ComponentLineDto> components;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComponentLineDto {
        private String componentCode;
        private String componentName;
        private String componentType;
        /** Null or masked when maskAmounts is true; otherwise the effective amount. */
        private BigDecimal amount;
        private String valueType;
    }
}
