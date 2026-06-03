package com.easyops.pharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposits", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Deposit implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "territory_id", nullable = false)
    private UUID territoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", insertable = false, updatable = false)
    private Territory territory;
    
    @Column(name = "employee_id")
    private UUID employeeId; // Optional - for reference/tracking only
    
    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month; // 1-12
    
    @Column(name = "deposit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal depositAmount; // Total deposit amount
    
    @Column(name = "bank_account_id")
    private UUID bankAccountId; // FK to accounting.bank_accounts - links to configured bank
    
    @Column(name = "bank_name", length = 200, nullable = false)
    private String bankName; // Populated from selected bank account for display
    
    @Column(name = "bank_account_number", length = 100, nullable = false)
    private String bankAccountNumber; // Populated from selected bank account for display
    
    @Column(name = "total_product_amount", precision = 19, scale = 2)
    private BigDecimal totalProductAmount; // Legacy; deposit amount entry is amount-only now
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, COMPLETED
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}

