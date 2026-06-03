package com.easyops.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequest {
    @NotNull(message = "Bank account ID is required")
    private UUID bankAccountId;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    @NotBlank(message = "Transaction type is required")
    private String transactionType;
    
    private String referenceNumber;
    private String description;
    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private UUID glJournalId;
    /** Counterpart GL account when posting this movement to the general ledger. */
    private UUID offsetGlAccountId;
    private Boolean postToGl = false;
    private UUID createdBy;
}

