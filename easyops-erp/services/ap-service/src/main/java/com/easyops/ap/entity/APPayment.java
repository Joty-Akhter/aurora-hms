package com.easyops.ap.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ap_payments", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class APPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "payment_number", nullable = false, unique = true, length = 50)
    private String paymentNumber;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Column(name = "check_number", length = 50)
    private String checkNumber;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "bank_account_id")
    private UUID bankAccountId;
    
    @Column(name = "gl_journal_id")
    private UUID glJournalId;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "status", length = 20)
    private String status = "DRAFT";
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<APPaymentAllocation> allocations = new ArrayList<>();
    
    public void addAllocation(APPaymentAllocation allocation) {
        allocations.add(allocation);
        allocation.setPayment(this);
    }
}

