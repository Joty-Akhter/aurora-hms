package com.easyops.ar.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ar_credit_notes", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ARCreditNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false, unique = true, length = 50)
    private String creditNoteNumber;
    
    @Column(nullable = false)
    private LocalDate creditNoteDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ARInvoice invoice;  // Reference invoice if credit note is for a specific invoice
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private CreditNoteReason reason;
    
    @Column(length = 1000)
    private String notes;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private CreditNoteStatus status = CreditNoteStatus.DRAFT;

    @Column(name = "gl_journal_id")
    private UUID glJournalId;
    
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ARCreditNoteLine> lines = new ArrayList<>();
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(length = 100)
    private String createdBy;
    
    @Column(length = 100)
    private String updatedBy;
    
    public enum CreditNoteReason {
        RETURN,
        DAMAGE,
        PRICING_ERROR,
        DISCOUNT,
        CANCELLATION,
        OTHER
    }
    
    public enum CreditNoteStatus {
        DRAFT,
        POSTED,
        APPLIED,
        CANCELLED
    }
}

