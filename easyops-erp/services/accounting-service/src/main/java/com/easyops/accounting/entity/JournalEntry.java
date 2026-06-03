package com.easyops.accounting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries", schema = "accounting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "journal_number", unique = true, nullable = false, length = 50)
    private String journalNumber;
    
    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;
    
    @Column(name = "period_id", nullable = false)
    private UUID periodId;
    
    @Column(name = "journal_type", length = 50)
    private String journalType = "MANUAL"; // MANUAL, SYSTEM, RECURRING, ADJUSTMENT
    
    @Column(name = "source_module", length = 50)
    private String sourceModule;
    
    @Column(name = "source_document_id")
    private UUID sourceDocumentId;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status", length = 20)
    private String status = "DRAFT"; // DRAFT, POSTED, REVERSED, CANCELLED
    
    @Column(name = "total_debit", precision = 19, scale = 4)
    private BigDecimal totalDebit = BigDecimal.ZERO;
    
    @Column(name = "total_credit", precision = 19, scale = 4)
    private BigDecimal totalCredit = BigDecimal.ZERO;
    
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    
    @Column(name = "posted_by")
    private UUID postedBy;
    
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
    
    @Column(name = "reversed_by")
    private UUID reversedBy;
    
    @Column(name = "reversal_journal_id")
    private UUID reversalJournalId;
    
    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @JsonIgnore
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<JournalLine> lines = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

