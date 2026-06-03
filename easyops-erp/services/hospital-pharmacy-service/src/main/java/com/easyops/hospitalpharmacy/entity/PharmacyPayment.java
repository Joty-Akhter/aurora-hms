package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_payments", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PharmacyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private PharmacyCreditAccount creditAccount;

    @Column(name = "dispense_order_id")
    private UUID dispenseOrderId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_mode", length = 50, nullable = false)
    private String paymentMode;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private OffsetDateTime paymentDate = OffsetDateTime.now();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
