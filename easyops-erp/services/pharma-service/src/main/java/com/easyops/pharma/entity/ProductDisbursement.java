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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_disbursements", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductDisbursement implements Serializable {
    
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
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId; // Receiving employee (belongs to territory)
    
    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;
    
    @Column(name = "year", nullable = false)
    private Integer year; // Auto-calculated from date
    
    @Column(name = "month", nullable = false)
    private Integer month; // Auto-calculated from date (1-12)
    
    @Column(name = "previous_month_opening_total_due", precision = 19, scale = 2)
    private BigDecimal previousMonthOpeningTotalDue = BigDecimal.ZERO;
    
    @Column(name = "total_supply_amount", precision = 19, scale = 2)
    private BigDecimal totalSupplyAmount = BigDecimal.ZERO; // Current month supply
    
    @Column(name = "total_balance_amount", precision = 19, scale = 2)
    private BigDecimal totalBalanceAmount = BigDecimal.ZERO; // Total quantity value
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, COMPLETED
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "productDisbursement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDisbursementLine> disbursementLines;
    
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

