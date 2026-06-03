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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "adjustment_lines", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AdjustmentLine implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "adjustment_id", nullable = false)
    private UUID adjustmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id", insertable = false, updatable = false)
    private Adjustment adjustment;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId; // Reference to inventory-service Product
    
    @Column(name = "product_name", length = 500)
    private String productName; // Denormalized for reference
    
    @Column(name = "pack_size", precision = 10, scale = 2)
    private BigDecimal packSize; // Auto-filled from product master
    
    @Column(name = "tp_with_vat", precision = 19, scale = 4)
    private BigDecimal tpWithVat; // Trade Price with VAT, auto-filled
    
    @Column(name = "mrp", precision = 19, scale = 4)
    private BigDecimal mrp; // Maximum Retail Price, auto-filled
    
    @Column(name = "current_outstanding_quantity", precision = 19, scale = 4)
    private BigDecimal currentOutstandingQuantity; // Displayed automatically
    
    @Column(name = "adjustment_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal adjustmentQuantity;
    
    @Column(name = "adjustment_amount", precision = 19, scale = 2)
    private BigDecimal adjustmentAmount; // Adjustment Quantity × TP with VAT (auto-calculated)
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

