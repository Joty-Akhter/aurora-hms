package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "epf_remittances", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "remittance_id")
    private UUID remittanceId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "remittance_month", nullable = false)
    private Integer remittanceMonth;

    @Column(name = "remittance_year", nullable = false)
    private Integer remittanceYear;

    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "pending"; // pending, posted_to_accounting, initiated, paid, failed, cancelled

    @Column(name = "liability_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal liabilityAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "payment_channel", length = 50)
    private String paymentChannel;

    @Column(name = "accounting_reference", length = 120)
    private String accountingReference;

    @Column(name = "accounting_posted_date")
    private LocalDate accountingPostedDate;

    @Column(name = "filing_id")
    private UUID filingId;

    @Column(name = "compliance_record_id")
    private UUID complianceRecordId;

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
