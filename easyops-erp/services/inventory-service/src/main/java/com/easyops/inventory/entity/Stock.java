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
@Table(name = "stock", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Stock implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    // Quantities
    @Column(name = "quantity_on_hand", precision = 19, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;
    
    @Column(name = "quantity_allocated", precision = 19, scale = 4)
    private BigDecimal quantityAllocated = BigDecimal.ZERO;
    
    @Column(name = "quantity_available", precision = 19, scale = 4)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;
    
    @Column(name = "quantity_on_order", precision = 19, scale = 4)
    private BigDecimal quantityOnOrder = BigDecimal.ZERO;
    
    @Column(name = "quantity_in_transit", precision = 19, scale = 4)
    private BigDecimal quantityInTransit = BigDecimal.ZERO;
    
    // Batch/Lot/Serial
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    @Column(name = "lot_number", length = 100)
    private String lotNumber;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    // Expiry
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    // Costing
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;
    
    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(name = "valuation_method", length = 20)
    private String valuationMethod = "FIFO";
    
    // Status
    @Column(name = "status", length = 50)
    private String status = "AVAILABLE";
    
    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;
    
    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

