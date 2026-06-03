package com.easyops.pharma.entity;

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
@Table(name = "target_coverage", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TargetCoverage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "target_id", nullable = false)
    private UUID targetId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", insertable = false, updatable = false)
    private Target target;
    
    @Column(name = "territory_id", nullable = false)
    private UUID territoryId;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month; // 1-12
    
    @Column(name = "target_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal targetAmount; // Monthly target
    
    @Column(name = "covered_amount", precision = 19, scale = 2)
    private BigDecimal coveredAmount = BigDecimal.ZERO; // Total deposits in month
    
    @Column(name = "coverage_percentage", precision = 5, scale = 2)
    private BigDecimal coveragePercentage; // (Covered / Target) × 100
    
    @Column(name = "status", length = 50)
    private String status; // ACHIEVED, NOT_ACHIEVED, EXCEEDED
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

