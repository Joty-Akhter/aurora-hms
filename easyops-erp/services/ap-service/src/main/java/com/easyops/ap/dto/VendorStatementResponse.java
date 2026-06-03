package com.easyops.ap.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorStatementResponse {
    
    private UUID vendorId;
    private String vendorName;
    private String vendorCode;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<StatementTransaction> transactions = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementTransaction {
        private LocalDate date;
        private String type;  // BILL, DEBIT_NOTE, PAYMENT
        private String reference;
        private String description;
        private BigDecimal debit;
        private BigDecimal credit;
        private BigDecimal balance;
    }
}

