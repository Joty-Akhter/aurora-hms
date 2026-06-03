package com.easyops.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseReceiptRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "Purchase Order ID is required")
    private UUID purchaseOrderId;
    
    private String poNumber;
    
    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    
    @NotNull(message = "Receipt date is required")
    private LocalDate receiptDate;
    
    private UUID supplierId;
    
    @NotNull(message = "Receipt lines are required")
    private List<ReceiptLine> lines;
    
    private String notes;
    private UUID createdBy;
    
    @Data
    public static class ReceiptLine {
        @NotNull
        private UUID productId;
        
        @NotNull
        @Positive
        private BigDecimal quantity;
        
        @NotNull
        @Positive
        private BigDecimal unitCost;
        
        private String batchNumber;
        private String lotNumber;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
        private String serialNumber;
    }
}

