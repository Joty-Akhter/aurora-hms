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
@Table(name = "quotations", schema = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Quotation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "quotation_number", nullable = false, length = 50)
    private String quotationNumber;
    
    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;
    
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;
    
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
    private String status = "DRAFT"; // DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;
    
    @Column(name = "sales_person_id")
    private UUID salesPersonId;
    
    @Column(name = "sales_person_name")
    private String salesPersonName;
    
    @Column(name = "converted_to_order_id")
    private UUID convertedToOrderId;
    
    @Column(name = "converted_date")
    private LocalDateTime convertedDate;
    
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<QuotationLine> lines = new ArrayList<>();
    
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
    public void addLine(QuotationLine line) {
        lines.add(line);
        line.setQuotation(this);
    }
    
    public void removeLine(QuotationLine line) {
        lines.remove(line);
        line.setQuotation(null);
    }
    
    public void calculateTotals() {
        this.subtotal = lines.stream()
                .map(QuotationLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Apply discount
        if (this.discountPercent != null && this.discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = this.subtotal.multiply(this.discountPercent).divide(BigDecimal.valueOf(100));
        }
        
        BigDecimal amountAfterDiscount = this.subtotal.subtract(this.discountAmount);
        
        // Calculate tax
        this.taxAmount = lines.stream()
                .map(QuotationLine::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = amountAfterDiscount.add(this.taxAmount);
    }
}

