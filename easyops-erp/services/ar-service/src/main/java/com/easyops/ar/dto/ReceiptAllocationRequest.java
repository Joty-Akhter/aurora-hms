package com.easyops.ar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReceiptAllocationRequest {
    
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;
    
    @NotNull(message = "Allocated amount is required")
    private BigDecimal allocatedAmount;
}

