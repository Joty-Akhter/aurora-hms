package com.easyops.ap.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ap_bills", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APBill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    private String billNumber;
    
    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Column(name = "period_id", nullable = false)
    private UUID periodId;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "subtotal", precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(name = "balance_due", precision = 19, scale = 4)
    private BigDecimal balanceDue = BigDecimal.ZERO;
    
    @Column(name = "status", length = 20)
    private String status = "DRAFT";
    
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "UNPAID";
    
    @Column(name = "gl_journal_id")
    private UUID glJournalId;
    
    @Column(name = "purchase_order_ref", length = 100)
    private String purchaseOrderRef;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<APBillLine> lines = new ArrayList<>();
    
    public void addLine(APBillLine line) {
        lines.add(line);
        line.setBill(this);
    }
}

