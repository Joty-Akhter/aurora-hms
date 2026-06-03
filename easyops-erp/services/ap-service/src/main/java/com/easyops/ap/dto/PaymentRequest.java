package com.easyops.ap.dto;

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
public class PaymentRequest {
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Payment number is required")
    private String paymentNumber;
    
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
    
    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;
    
    private String paymentMethod;
    private String referenceNumber;
    private String checkNumber;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    private String currency = "USD";
    private UUID bankAccountId;
    private String notes;
    
    @Valid
    private List<PaymentAllocationRequest> allocations = new ArrayList<>();
    
    private UUID createdBy;
}

