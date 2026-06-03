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
@Table(name = "reorder_alerts", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReorderAlert implements Serializable {
    
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
    
    @Column(name = "reorder_rule_id", nullable = false)
    private UUID reorderRuleId;
    
    @Column(name = "current_quantity", precision = 19, scale = 4)
    private BigDecimal currentQuantity;
    
    @Column(name = "reorder_point", precision = 19, scale = 4)
    private BigDecimal reorderPoint;
    
    @Column(name = "suggested_order_qty", precision = 19, scale = 4)
    private BigDecimal suggestedOrderQty;
    
    @Column(name = "alert_status", length = 50)
    private String alertStatus = "OPEN"; // OPEN, ACKNOWLEDGED, PO_CREATED, CLOSED
    
    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "suggested_po_id")
    private UUID suggestedPoId;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

