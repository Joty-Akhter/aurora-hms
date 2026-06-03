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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reorder_rules", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReorderRule implements Serializable {
    
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
    
    @Column(name = "reorder_point", precision = 19, scale = 4, nullable = false)
    private BigDecimal reorderPoint;
    
    @Column(name = "reorder_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal reorderQuantity;
    
    @Column(name = "min_quantity", precision = 19, scale = 4)
    private BigDecimal minQuantity;
    
    @Column(name = "max_quantity", precision = 19, scale = 4)
    private BigDecimal maxQuantity;
    
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;
    
    @Column(name = "safety_stock", precision = 19, scale = 4)
    private BigDecimal safetyStock = BigDecimal.ZERO;
    
    @Column(name = "preferred_supplier_id")
    private UUID preferredSupplierId;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "auto_create_po")
    private Boolean autoCreatePo = false;
    
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    
    @Column(name = "trigger_count")
    private Integer triggerCount = 0;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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
}

