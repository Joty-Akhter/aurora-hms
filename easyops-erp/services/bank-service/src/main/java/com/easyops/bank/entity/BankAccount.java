package com.easyops.bank.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;
    
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "branch_name")
    private String branchName;
    
    @Column(name = "account_type", length = 50)
    private String accountType = "CHECKING";
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "gl_account_id")
    private UUID glAccountId;
    
    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "current_balance", precision = 19, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

