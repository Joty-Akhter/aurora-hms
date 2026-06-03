package com.easyops.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provide exactly one of {@code newQuantity} (target on-hand after adjustment)
 * or {@code quantityDelta} (change applied to current on-hand: positive adds, negative removes).
 */
@Data
public class StockAdjustmentRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    
    /** Target on-hand quantity after adjustment (mutually exclusive with quantityDelta). */
    private BigDecimal newQuantity;
    
    /** Added to current on-hand (mutually exclusive with newQuantity). */
    private BigDecimal quantityDelta;
    
    @NotNull(message = "Reason is required")
    private String reason;
    
    private String notes;
    private UUID createdBy;
}

