package com.easyops.hospital.entity;

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
@Table(name = "prescription_refills", schema = "ehr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PrescriptionRefill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "refill_id")
    private UUID refillId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refill_request_id")
    private PrescriptionRefillRequest refillRequest;
    
    // Refill Identification
    @Column(name = "refill_number", nullable = false)
    private Integer refillNumber;
    
    @Column(name = "refill_date", nullable = false)
    private LocalDate refillDate;
    
    // Dispensing Information
    @Column(name = "quantity_dispensed", precision = 10, scale = 2)
    private BigDecimal quantityDispensed;
    
    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit;
    
    @Column(name = "pharmacy_id")
    private UUID pharmacyId;
    
    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;
    
    @Column(name = "pharmacy_npi", length = 20)
    private String pharmacyNpi;
    
    // Filling Information
    @Column(name = "filled_by")
    private UUID filledBy;
    
    @Column(name = "filled_by_name", length = 200)
    private String filledByName;
    
    @Column(name = "filled_date")
    @Builder.Default
    private LocalDateTime filledDate = LocalDateTime.now();
    
    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}
