package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@Table(name = "loan_installments", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "sequence_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "installment_id")
    private UUID installmentId;

    @Column(name = "loan_id", nullable = false, updatable = false)
    private UUID loanId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "scheduled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal scheduledAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanInstallmentStatus status = LoanInstallmentStatus.DUE;

    /** RP-01: required when status is SKIPPED. */
    @Column(name = "skip_reason", length = 2000)
    private String skipReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
