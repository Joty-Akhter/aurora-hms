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
@Table(name = "epf_transfers", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfTransfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transfer_id")
    private UUID transferId;
    
    @Column(name = "source_epf_account_id", nullable = false)
    private UUID sourceEpfAccountId;
    
    @Column(name = "target_epf_account_id")
    private UUID targetEpfAccountId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "transfer_type", nullable = false, length = 50)
    private String transferType; // inter_organization, intra_organization
    
    @Column(name = "transfer_amount", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal transferAmount;
    
    @Column(name = "transfer_date")
    private LocalDate transferDate;
    
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "pending"; // pending, processed, failed
    
    @Column(name = "source_uan_number", length = 50)
    private String sourceUanNumber;
    
    @Column(name = "target_uan_number", length = 50)
    private String targetUanNumber;
    
    @Column(name = "transfer_reference", length = 100)
    private String transferReference;
    
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

