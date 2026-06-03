package com.easyops.bank.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "reconciliation_items", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private BankTransaction transaction;
    
    @Column(name = "is_matched")
    private Boolean isMatched = false;
    
    @Column(name = "match_type", length = 50)
    private String matchType;
}

