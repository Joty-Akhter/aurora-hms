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
@Table(name = "stock_movements", schema = "hospital_pharmacy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_location_id", nullable = false)
    private PharmacyLocation pharmacyLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @CreatedDate
    @Column(name = "movement_time", nullable = false, updatable = false)
    private OffsetDateTime movementTime;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "requested_by")
    private UUID requestedBy;
}

