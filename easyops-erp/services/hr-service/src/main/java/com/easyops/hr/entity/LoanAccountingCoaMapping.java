package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PI-05 optional: organization chart-of-account codes for loan accounting export lines.
 */
@Entity
@Table(name = "loan_accounting_coa_mappings", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "mapping_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanAccountingCoaMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mapping_id")
    private UUID mappingId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "mapping_key", nullable = false, length = 64)
    private String mappingKey;

    @Column(name = "debit_account_code", nullable = false, length = 64)
    private String debitAccountCode;

    @Column(name = "credit_account_code", nullable = false, length = 64)
    private String creditAccountCode;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
