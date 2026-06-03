package com.easyops.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ReconciliationRequest {
    @NotNull(message = "Bank account ID is required")
    private UUID bankAccountId;
    
    @NotNull(message = "Statement date is required")
    private LocalDate statementDate;
    
    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;
    
    @NotNull(message = "Closing balance is required")
    private BigDecimal closingBalance;
    
    private String notes;
    private List<UUID> matchedTransactionIds = new ArrayList<>();
    private UUID reconciledBy;
}

