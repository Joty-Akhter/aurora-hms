package com.easyops.bank.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_reconciliations", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;
    
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;
    
    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance = BigDecimal.ZERO;
    
    @Column(name = "book_balance", precision = 19, scale = 4)
    private BigDecimal bookBalance = BigDecimal.ZERO;
    
    @Column(name = "difference", precision = 19, scale = 4)
    private BigDecimal difference = BigDecimal.ZERO;
    
    @Column(name = "status", length = 20)
    private String status = "IN_PROGRESS";
    
    @Column(name = "reconciled_by")
    private UUID reconciledBy;
    
    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

