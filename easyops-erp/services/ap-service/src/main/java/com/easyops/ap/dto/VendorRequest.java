package com.easyops.ap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VendorRequest {
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Vendor code is required")
    private String vendorCode;
    
    @NotBlank(message = "Vendor name is required")
    private String vendorName;
    
    private String email;
    private String phone;
    private Integer paymentTerms = 30;
    private String taxId;
    private Boolean isActive = true;
}

