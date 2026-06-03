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
@Table(name = "targets", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Target implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "territory_id", nullable = false)
    private UUID territoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", insertable = false, updatable = false)
    private Territory territory;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId; // Manager assigned to target (belongs to territory)
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "start_month", nullable = false)
    private Integer startMonth; // 1-12
    
    @Column(name = "end_month", nullable = false)
    private Integer endMonth; // 1-12
    
    @Column(name = "target_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal targetAmount; // Monthly target amount
    
    @Column(name = "status", length = 50)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, COMPLETED
    
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

