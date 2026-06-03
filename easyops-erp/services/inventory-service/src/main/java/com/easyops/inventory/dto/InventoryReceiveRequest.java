package com.easyops.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InventoryReceiveRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    
    @NotEmpty(message = "At least one product is required")
    @Valid
    private List<ReceiveItem> items;
    
    private String notes;
    private UUID createdBy;
    
    @Data
    public static class ReceiveItem {
        @NotNull(message = "Product ID is required")
        private UUID productId;
        
        @NotNull(message = "Quantity is required")
        private java.math.BigDecimal quantity;
        
        @NotNull(message = "Pack size is required")
        private java.math.BigDecimal packSize;
        
        @NotNull(message = "Trade Price is required")
        private java.math.BigDecimal tradePrice;
        
        @NotNull(message = "MRP is required")
        private java.math.BigDecimal mrp;
        
        private java.time.LocalDate expiryDate;
        
        private String notes;
    }
}

