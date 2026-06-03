package com.easyops.pharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "incentive_distributions", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IncentiveDistribution implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "incentive_calculation_id", nullable = false)
    private UUID incentiveCalculationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incentive_calculation_id", insertable = false, updatable = false)
    @JsonIgnore
    private IncentiveCalculation incentiveCalculation;
    
    @Column(name = "employee_id")
    private UUID employeeId; // Reference to HR module employee; null for DEVELOPMENT_FUND
    
    @Column(name = "territory_id", nullable = false)
    private UUID territoryId;
    
    @Column(name = "role_in_area", length = 50)
    private String roleInArea; // SR, MPO, Manager (schema column name retained)
    
    @Column(name = "incentive_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal incentiveAmount;
    
    @Column(name = "distribution_type", length = 50)
    private String distributionType; // SR_SHARE, MPO_SHARE, MANAGER_SHARE
    
    @Column(name = "years_of_service")
    private Integer yearsOfService; // For MPO eligibility
    
    @Column(name = "calculation_date")
    private LocalDateTime calculationDate;
    
    @Column(name = "paid_date")
    private LocalDateTime paidDate;
    
    @Column(name = "status", length = 50)
    private String status = "CALCULATED"; // CALCULATED, PAID
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

