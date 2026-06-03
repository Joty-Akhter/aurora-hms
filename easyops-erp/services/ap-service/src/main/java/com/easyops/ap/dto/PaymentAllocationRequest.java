package com.easyops.ap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentAllocationRequest {
    @NotNull(message = "Bill ID is required")
    private UUID billId;
    
    @NotNull(message = "Allocated amount is required")
    private BigDecimal allocatedAmount;
}

