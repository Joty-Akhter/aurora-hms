package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_receipt_reprint_audit", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PharmacyReceiptReprintAudit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispense_order_id", nullable = false)
    private DispenseOrder dispenseOrder;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Receipt access timestamp (auditing maps to {@code printed_at}). */
    @CreatedDate
    @Column(name = "printed_at", nullable = false, updatable = false)
    private OffsetDateTime printedAt;

    @Column(name = "duplicate_of_previous", nullable = false)
    @Builder.Default
    private boolean duplicateOfPrevious = false;
}
