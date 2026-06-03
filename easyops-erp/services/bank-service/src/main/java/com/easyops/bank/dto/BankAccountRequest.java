package com.easyops.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BankAccountRequest {
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    private String bankName;
    private String branchName;
    private String accountType = "CHECKING";
    private String currency = "USD";
    
    @NotNull(message = "GL Account ID is required")
    private UUID glAccountId;
    
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private Boolean isActive = true;
}

