package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_slabs", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TaxSlab {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tax_slab_id")
    private UUID taxSlabId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "slab_name", nullable = false, length = 100)
    private String slabName;
    
    @Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minAmount;
    
    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;
    
    @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    @Column(name = "fixed_amount", precision = 10, scale = 2)
    private BigDecimal fixedAmount = BigDecimal.ZERO;
    
    @Column(name = "effective_year", nullable = false)
    private Integer effectiveYear;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

