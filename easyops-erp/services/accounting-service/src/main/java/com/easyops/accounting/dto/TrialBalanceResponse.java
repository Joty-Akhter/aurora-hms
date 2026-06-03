package com.easyops.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceResponse {
    private String accountCode;
    private String accountName;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private BigDecimal closingBalance;
}

