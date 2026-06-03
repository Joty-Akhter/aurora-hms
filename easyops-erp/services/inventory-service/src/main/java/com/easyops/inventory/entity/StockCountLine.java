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
@Table(name = "stock_count_lines", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockCountLine implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "count_id", nullable = false)
    private UUID countId;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    @Column(name = "system_quantity", precision = 19, scale = 4)
    private BigDecimal systemQuantity = BigDecimal.ZERO;
    
    @Column(name = "counted_quantity", precision = 19, scale = 4)
    private BigDecimal countedQuantity;
    
    @Column(name = "variance_quantity", precision = 19, scale = 4)
    private BigDecimal varianceQuantity = BigDecimal.ZERO;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;
    
    @Column(name = "variance_value", precision = 19, scale = 4)
    private BigDecimal varianceValue = BigDecimal.ZERO;
    
    @Column(name = "variance_reason")
    private String varianceReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "counted_at")
    private LocalDateTime countedAt;
    
    @Column(name = "counted_by")
    private UUID countedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

