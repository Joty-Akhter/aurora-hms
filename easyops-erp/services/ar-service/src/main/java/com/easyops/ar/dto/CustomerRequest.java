package com.easyops.ar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CustomerRequest {
    
    // Optional: For syncing customers from other services (e.g., sales)
    private UUID id;
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Customer code is required")
    private String customerCode;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    private String email;
    
    private String phone;
    
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    private Integer paymentTerms = 30;
    
    private Boolean isActive = true;
}

