package com.easyops.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunity_products", schema = "crm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "opp_product_id")
    private UUID oppProductId;
    
    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "product_code", length = 100)
    private String productCode;
    
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;
    
    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;
    
    @Column(name = "quantity", precision = 15, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;
    
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;
    
    @Column(name = "subtotal", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal subtotal;
    
    @Column(name = "discount_total", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal discountTotal;
    
    @Column(name = "tax_amount", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal taxAmount;
    
    @Column(name = "line_total", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "line_order")
    private Integer lineOrder = 1;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

