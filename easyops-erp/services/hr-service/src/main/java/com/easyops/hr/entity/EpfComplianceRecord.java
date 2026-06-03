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
@Table(name = "epf_compliance_records", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfComplianceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "compliance_record_id")
    private UUID complianceRecordId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "compliance_type", nullable = false, length = 100)
    private String complianceType; // monthly_return, annual_return, challan
    
    @Column(name = "compliance_period_start", nullable = false)
    private LocalDate compliancePeriodStart;
    
    @Column(name = "compliance_period_end", nullable = false)
    private LocalDate compliancePeriodEnd;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "submission_date")
    private LocalDate submissionDate;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "pending"; // pending, submitted, verified, rejected
    
    @Column(name = "file_reference", length = 255)
    private String fileReference;
    
    @Column(name = "amount", precision = 15, scale = 2)
    private java.math.BigDecimal amount;
    
    @Column(name = "penalty_amount", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal penaltyAmount = java.math.BigDecimal.ZERO;
    
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

