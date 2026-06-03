package com.easyops.hr.entity;

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
import java.util.UUID;

@Entity
@Table(name = "epf_withdrawals", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfWithdrawal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "withdrawal_id")
    private UUID withdrawalId;
    
    @Column(name = "epf_account_id", nullable = false)
    private UUID epfAccountId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "withdrawal_type", nullable = false, length = 50)
    private String withdrawalType; // partial, full, advance, pension
    
    @Column(name = "request_date")
    private LocalDate requestDate;
    
    @Column(name = "withdrawal_reason", length = 200)
    private String withdrawalReason;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal requestedAmount;
    
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private java.math.BigDecimal approvedAmount;
    
    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "pending"; // pending, approved, rejected, processed, cancelled
    
    @Column(name = "approval_workflow", columnDefinition = "JSONB")
    private String approvalWorkflow;
    
    @Column(name = "processed_date")
    private LocalDate processedDate;
    
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

