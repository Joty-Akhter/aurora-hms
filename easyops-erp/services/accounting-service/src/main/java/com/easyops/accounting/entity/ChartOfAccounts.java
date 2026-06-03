package com.easyops.accounting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chart_of_accounts", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccounts implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;
    
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    @Column(name = "parent_account_id")
    private UUID parentAccountId;
    
    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    
    @Column(name = "account_category", length = 100)
    private String accountCategory;
    
    @Column(name = "account_subcategory", length = 100)
    private String accountSubcategory;
    
    @Column(name = "level")
    private Integer level = 1;
    
    @Column(name = "is_group")
    private Boolean isGroup = false;
    
    @Column(name = "is_system_account")
    private Boolean isSystemAccount = false;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;
    
    @Column(name = "current_balance", precision = 19, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "allow_manual_entry")
    private Boolean allowManualEntry = true;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "tax_type", length = 50)
    private String taxType;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

