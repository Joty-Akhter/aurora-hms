package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_transfers", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockTransfer implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;
    
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    
    @Column(name = "from_warehouse_id", nullable = false)
    private UUID fromWarehouseId;
    
    @Column(name = "to_warehouse_id", nullable = false)
    private UUID toWarehouseId;
    
    @Column(name = "status", length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, APPROVED, IN_TRANSIT, RECEIVED, CANCELLED
    
    @Column(name = "priority", length = 20)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    
    @Column(name = "transfer_type", length = 50)
    private String transferType = "STANDARD"; // STANDARD, EMERGENCY, REPLENISHMENT
    
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "shipped_by")
    private UUID shippedBy;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "received_by")
    private UUID receivedBy;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;
    
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
    
    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @OneToMany(mappedBy = "transferId", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("transfer")
    private List<StockTransferLine> lines = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public void addLine(StockTransferLine line) {
        lines.add(line);
        line.setTransferId(this.id);
    }
}

