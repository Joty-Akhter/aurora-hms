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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_counts", schema = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockCount implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "count_number", nullable = false, unique = true, length = 50)
    private String countNumber;
    
    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;
    
    @Column(name = "count_type", length = 50)
    private String countType = "FULL"; // FULL, CYCLE, SPOT
    
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    
    @Column(name = "status", length = 50)
    private String status = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED, APPROVED, CANCELLED
    
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "counted_by")
    private UUID countedBy;
    
    @Column(name = "verified_by")
    private UUID verifiedBy;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "total_variance_value", precision = 19, scale = 4)
    private BigDecimal totalVarianceValue = BigDecimal.ZERO;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "countId", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("count")
    private List<StockCountLine> lines = new ArrayList<>();
    
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
    
    public void addLine(StockCountLine line) {
        lines.add(line);
        line.setCountId(this.id);
    }
}

