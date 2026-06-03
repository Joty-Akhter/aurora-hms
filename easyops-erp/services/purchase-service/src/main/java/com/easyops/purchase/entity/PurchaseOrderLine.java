package com.easyops.purchase.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines", schema = "purchase")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PurchaseOrderLine implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    @JsonIgnoreProperties({"lines", "hibernateLazyInitializer", "handler"})
    private PurchaseOrder purchaseOrder;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;
    
    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;
    
    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;
    
    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure = "EA";
    
    @Column(name = "unit_price", precision = 15, scale = 4, nullable = false)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "line_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal lineTotal;
    
    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;
    
    @Column(name = "promised_delivery_date")
    private LocalDate promisedDeliveryDate;
    
    @Column(name = "warehouse_id")
    private UUID warehouseId;
    
    @Column(name = "warehouse_name")
    private String warehouseName;
    
    @Column(name = "received_quantity", precision = 15, scale = 3)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;
    
    @Column(name = "pending_quantity", precision = 15, scale = 3)
    private BigDecimal pendingQuantity;
    
    @Column(name = "invoiced_quantity", precision = 15, scale = 3)
    private BigDecimal invoicedQuantity = BigDecimal.ZERO;
    
    @Column(name = "status", length = 50)
    private String status = "PENDING";
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

