package com.easyops.hospitalpharmacy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispense_lines", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DispenseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispense_order_id", nullable = false)
    private DispenseOrder dispenseOrder;

    @Column(name = "prescription_line_id")
    private UUID prescriptionLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "quantity_prescribed", precision = 19, scale = 4)
    private BigDecimal quantityPrescribed;

    @Column(name = "quantity_dispensed", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDispensed;

    @Column(name = "quantity_returned", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityReturned;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "documenting_user_id")
    private UUID documentingUserId;

    @Column(name = "stock_snapshot_ref", length = 500)
    private String stockSnapshotRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substituted_drug_id")
    private Drug substitutedDrug;

    @Column(name = "override_reason_code", length = 2000)
    private String overrideReasonCode;

    /** Phase P3 WS-G — reason for dispensing a restricted formulary drug without using a preferred alternative. */
    @Column(name = "formulary_override_reason", length = 2000)
    private String formularyOverrideReason;

    @Column(name = "override_approver_id")
    private UUID overrideApproverId;

    @Column(name = "witness_user_id")
    private UUID witnessUserId;

    /** P4 WS-I — pharmacist attestation when dispensing despite interaction/allergy block. */
    @Column(name = "clinical_safety_override_reason", length = 2000)
    private String clinicalSafetyOverrideReason;

    @Column(name = "remaining_quantity", precision = 19, scale = 4)
    private BigDecimal remainingQuantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Status {
        PENDING,
        DISPENSED,
        PARTIALLY_DISPENSED,
        /** Dispensed despite insufficient recorded stock — {@code pharmacy.md} §4.1.5 */
        FILLED_WITH_STOCK_OVERRIDE,
        REFUSED,
        OUT_OF_STOCK,
        NOT_STARTED,
        RETURNED,
        CANCELLED
    }
}

