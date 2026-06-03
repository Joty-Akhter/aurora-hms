package com.easyops.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales_orders", schema = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SalesOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;
    
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "customer_email")
    private String customerEmail;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;
    
    @Column(name = "subtotal", precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT"; // DRAFT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, INVOICED
    
    @Column(name = "payment_status", length = 50)
    private String paymentStatus = "UNPAID"; // UNPAID, PARTIALLY_PAID, PAID
    
    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus = "NOT_DELIVERED"; // NOT_DELIVERED, PARTIALLY_DELIVERED, DELIVERED
    
    @Column(name = "priority", length = 50)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;
    
    @Column(name = "sales_person_id")
    private UUID salesPersonId;
    
    @Column(name = "sales_person_name")
    private String salesPersonName;
    
    @Column(name = "quotation_id")
    private UUID quotationId;
    
    @Column(name = "converted_to_invoice_id")
    private UUID convertedToInvoiceId;
    
    @Column(name = "converted_to_invoice_date")
    private LocalDateTime convertedToInvoiceDate;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SalesOrderLine> lines = new ArrayList<>();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public void addLine(SalesOrderLine line) {
        lines.add(line);
        line.setSalesOrder(this);
    }
    
    public void removeLine(SalesOrderLine line) {
        lines.remove(line);
        line.setSalesOrder(null);
    }
    
    public void calculateTotals() {
        this.subtotal = lines.stream()
                .map(SalesOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Apply discount
        if (this.discountPercent != null && this.discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = this.subtotal.multiply(this.discountPercent).divide(BigDecimal.valueOf(100));
        }
        
        BigDecimal amountAfterDiscount = this.subtotal.subtract(this.discountAmount);
        
        // Calculate tax
        this.taxAmount = lines.stream()
                .map(SalesOrderLine::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = amountAfterDiscount.add(this.taxAmount);
    }
}

