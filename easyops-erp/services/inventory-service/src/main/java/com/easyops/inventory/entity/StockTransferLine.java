package com.easyops.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_lines", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockTransferLine implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "from_location_id")
    private UUID fromLocationId;
    
    @Column(name = "to_location_id")
    private UUID toLocationId;
    
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    @Column(name = "requested_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal requestedQuantity;
    
    @Column(name = "shipped_quantity", precision = 19, scale = 4)
    private BigDecimal shippedQuantity = BigDecimal.ZERO;
    
    @Column(name = "received_quantity", precision = 19, scale = 4)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;
    
    @Column(name = "variance_quantity", precision = 19, scale = 4)
    private BigDecimal varianceQuantity = BigDecimal.ZERO;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;
    
    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING, SHIPPED, RECEIVED, CANCELLED
    
    @Column(name = "variance_reason", columnDefinition = "TEXT")
    private String varianceReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

