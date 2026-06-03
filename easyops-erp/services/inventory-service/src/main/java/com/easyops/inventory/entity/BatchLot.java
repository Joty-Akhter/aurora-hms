package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_lots", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BatchLot implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;
    
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "supplier_id")
    private UUID supplierId;
    
    @Column(name = "po_number", length = 50)
    private String poNumber;
    
    @Column(name = "receipt_date")
    private LocalDate receiptDate;
    
    @Column(name = "initial_quantity", precision = 19, scale = 4)
    private BigDecimal initialQuantity = BigDecimal.ZERO;
    
    @Column(name = "current_quantity", precision = 19, scale = 4)
    private BigDecimal currentQuantity = BigDecimal.ZERO;
    
    @Column(name = "status", length = 50)
    private String status = "ACTIVE"; // ACTIVE, QUARANTINE, EXPIRED, DEPLETED
    
    @Column(name = "quality_certificate", length = 500)
    private String qualityCertificate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

