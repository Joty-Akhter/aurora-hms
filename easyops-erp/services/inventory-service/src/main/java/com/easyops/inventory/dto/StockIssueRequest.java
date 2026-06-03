package com.easyops.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockIssueRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    
    private String sourceType; // SALES_ORDER, PRODUCTION, MANUAL
    private UUID sourceId;
    private String notes;
    private UUID createdBy;
}

