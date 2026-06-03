package com.easyops.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescription_history", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    private UUID historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    // Change Information
    @Column(name = "change_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    
    // Nullable: webhook-driven fill-status updates have no acting user.
    @Column(name = "changed_by")
    private UUID changedBy;
    
    @Column(name = "changed_date")
    @Builder.Default
    private LocalDateTime changedDate = LocalDateTime.now();
    
    // Change Details
    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    // Additional Context
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum ChangeType {
        CREATED, UPDATED, STATUS_CHANGED, SENT, FILLED, CANCELLED, 
        INTERACTION_ADDED, ALLERGY_WARNING_ADDED, VALIDATED, OTHER
    }
}
