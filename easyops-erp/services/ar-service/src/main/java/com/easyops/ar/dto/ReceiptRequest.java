package com.easyops.ar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ReceiptRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Receipt number is required")
    private String receiptNumber;
    
    @NotNull(message = "Receipt date is required")
    private LocalDate receiptDate;
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    private String paymentMethod;
    
    private String referenceNumber;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    private String currency = "USD";
    
    private UUID bankAccountId;
    
    private String notes;
    
    @Valid
    private List<ReceiptAllocationRequest> allocations = new ArrayList<>();
    
    private UUID createdBy;
}

