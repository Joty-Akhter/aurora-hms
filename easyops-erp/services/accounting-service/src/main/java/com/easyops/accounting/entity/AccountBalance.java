package com.easyops.accounting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(name = "period_id", nullable = false)
    private UUID periodId;
    
    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "debit_total", precision = 19, scale = 4)
    private BigDecimal debitTotal = BigDecimal.ZERO;
    
    @Column(name = "credit_total", precision = 19, scale = 4)
    private BigDecimal creditTotal = BigDecimal.ZERO;
    
    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance = BigDecimal.ZERO;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        // Calculate closing balance
        closingBalance = openingBalance.add(debitTotal).subtract(creditTotal);
    }
}

