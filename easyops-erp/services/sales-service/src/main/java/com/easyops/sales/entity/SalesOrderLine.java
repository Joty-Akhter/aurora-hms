package com.easyops.sales.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales_order_lines", schema = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SalesOrderLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @JsonBackReference
    private SalesOrder salesOrder;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "product_id")
    private UUID productId;
    
    @Column(name = "product_code", length = 50)
    private String productCode;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;
    
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;
    
    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "line_total", precision = 19, scale = 4)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    
    @Column(name = "delivered_quantity", precision = 19, scale = 4)
    private BigDecimal deliveredQuantity = BigDecimal.ZERO;
    
    @Column(name = "invoiced_quantity", precision = 19, scale = 4)
    private BigDecimal invoicedQuantity = BigDecimal.ZERO;
    
    @Column(name = "revenue_account_id")
    private UUID revenueAccountId;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper method to calculate line totals
    public void calculateLineTotal() {
        // Calculate base amount
        BigDecimal baseAmount = this.quantity.multiply(this.unitPrice);
        
        // Apply line discount
        if (this.discountPercent != null && this.discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = baseAmount.multiply(this.discountPercent).divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP);
        }
        
        BigDecimal amountAfterDiscount = baseAmount.subtract(this.discountAmount);
        
        // Calculate tax
        if (this.taxPercent != null && this.taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = amountAfterDiscount.multiply(this.taxPercent).divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP);
        }
        
        this.lineTotal = amountAfterDiscount.add(this.taxAmount);
    }
}

