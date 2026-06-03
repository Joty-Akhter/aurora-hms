package com.easyops.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralLedgerResponse {
    private LocalDate transactionDate;
    private String journalNumber;
    private String description;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
    private UUID journalEntryId;
    private UUID journalLineId;
    private String status;
    private String reference;
}

