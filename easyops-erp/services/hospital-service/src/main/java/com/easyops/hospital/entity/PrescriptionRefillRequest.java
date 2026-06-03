package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescription_refill_requests", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionRefillRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "refill_request_id")
    private UUID refillRequestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    // Request Information
    @Column(name = "request_source", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RequestSource requestSource;
    
    @Column(name = "request_date")
    @Builder.Default
    private LocalDateTime requestDate = LocalDateTime.now();
    
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    @Column(name = "requested_by_name", length = 200)
    private String requestedByName;
    
    // Pharmacy Information
    @Column(name = "pharmacy_id")
    private UUID pharmacyId;
    
    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;
    
    @Column(name = "pharmacy_npi", length = 20)
    private String pharmacyNpi;
    
    @Column(name = "pharmacy_phone", length = 50)
    private String pharmacyPhone;
    
    // Refill Details
    @Column(name = "refills_requested")
    @Builder.Default
    private Integer refillsRequested = 1;
    
    @Column(name = "refills_remaining")
    private Integer refillsRemaining;
    
    @Column(name = "last_fill_date")
    private LocalDate lastFillDate;
    
    @Column(name = "days_since_last_fill")
    private Integer daysSinceLastFill;
    
    // Request Status
    @Column(name = "request_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus requestStatus = RequestStatus.PENDING;
    
    // Approval Information
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;
    
    // Denial Information
    @Column(name = "denied_by")
    private UUID deniedBy;
    
    @Column(name = "denied_date")
    private LocalDateTime deniedDate;
    
    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;
    
    // Modification Information
    @Column(name = "modified_by")
    private UUID modifiedBy;
    
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
    
    @Column(name = "modification_notes", columnDefinition = "TEXT")
    private String modificationNotes;
    
    @Column(name = "original_refills_requested")
    private Integer originalRefillsRequested;
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "urgency_level", length = 20)
    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgencyLevel;
    
    // Auto-approval Information
    @Column(name = "was_auto_approved")
    @Builder.Default
    private Boolean wasAutoApproved = false;
    
    @Column(name = "auto_approval_rule_id")
    private UUID autoApprovalRuleId;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    // Relationships
    @OneToMany(mappedBy = "refillRequest", cascade = CascadeType.ALL)
    private List<PrescriptionRefill> refills;
    
    public enum RequestSource {
        PHARMACY, PATIENT, PROVIDER, SYSTEM, OTHER
    }
    
    public enum RequestStatus {
        PENDING, APPROVED, DENIED, MODIFIED, COMPLETED, CANCELLED
    }
    
    public enum UrgencyLevel {
        LOW, MEDIUM, HIGH, URGENT
    }
}
