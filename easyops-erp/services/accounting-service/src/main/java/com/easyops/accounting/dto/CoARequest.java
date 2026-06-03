package com.easyops.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CoARequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Account code is required")
    private String accountCode;
    
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    private UUID parentAccountId;
    
    @NotBlank(message = "Account type is required")
    private String accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    
    private String accountCategory;
    private String accountSubcategory;
    private Integer level = 1;
    private Boolean isGroup = false;
    private String currency = "USD";
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private LocalDate openingBalanceDate;
    private Boolean allowManualEntry = true;
    private String description;
    private String taxType;
    private String[] tags;
}

