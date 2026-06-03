package com.easyops.bank.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BankTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BankAccount bankAccount;
    
    @Column(name = "bank_account_id", insertable = false, updatable = false)
    private UUID bankAccountId;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "debit_amount", precision = 19, scale = 4)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    
    @Column(name = "credit_amount", precision = 19, scale = 4)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    
    @Column(name = "running_balance", precision = 19, scale = 4)
    private BigDecimal runningBalance = BigDecimal.ZERO;
    
    @Column(name = "is_reconciled")
    private Boolean isReconciled = false;
    
    @Column(name = "reconciliation_id")
    private UUID reconciliationId;
    
    @Column(name = "gl_journal_id")
    private UUID glJournalId;
    
    @Column(name = "status", length = 20)
    private String status = "POSTED";
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

