package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** ES-58: Total amount per component across employees (as-of snapshot) for budgeting and GL posting. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentWiseCostDto {
    private UUID componentId;
    private String componentCode;
    private String componentName;
    private String componentType;
    private String category;
    private BigDecimal totalAmount;
    private int employeeCount;
    private String currency;
}
