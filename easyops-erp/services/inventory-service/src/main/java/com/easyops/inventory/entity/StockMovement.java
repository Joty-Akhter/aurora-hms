package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockMovement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    // Transaction Details
    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType; // RECEIPT, ISSUE, ADJUSTMENT, TRANSFER_OUT, TRANSFER_IN, RETURN
    
    // Product & Location
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    // Quantities
    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;
    
    @Column(name = "uom", length = 50)
    private String uom;
    
    // Batch/Serial
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    // Cost
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;
    
    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    // Source References
    @Column(name = "source_type", length = 50)
    private String sourceType;
    
    @Column(name = "source_id")
    private UUID sourceId;
    
    @Column(name = "source_document_number", length = 100)
    private String sourceDocumentNumber;
    
    // Transfer specific
    @Column(name = "from_warehouse_id")
    private UUID fromWarehouseId;
    
    @Column(name = "from_location_id")
    private UUID fromLocationId;
    
    @Column(name = "to_warehouse_id")
    private UUID toWarehouseId;
    
    @Column(name = "to_location_id")
    private UUID toLocationId;
    
    @Column(name = "transfer_id")
    private UUID transferId;
    
    // Adjustment specific
    @Column(name = "adjustment_reason")
    private String adjustmentReason;
    
    @Column(name = "adjustment_type", length = 50)
    private String adjustmentType;
    
    // GL Integration
    @Column(name = "gl_journal_id")
    private UUID glJournalId;
    
    // Approval
    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;
    
    // Status
    @Column(name = "status", length = 50)
    private String status = "COMPLETED";
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}

