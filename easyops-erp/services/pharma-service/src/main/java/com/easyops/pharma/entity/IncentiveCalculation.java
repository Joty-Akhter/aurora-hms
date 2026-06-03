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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incentive_calculations", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IncentiveCalculation implements Serializable {
    
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
    @JsonIgnore
    private Territory territory;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month; // 1-12
    
    @Column(name = "target_amount", precision = 19, scale = 2)
    private BigDecimal targetAmount;
    
    @Column(name = "covered_amount", precision = 19, scale = 2)
    private BigDecimal coveredAmount;
    
    @Column(name = "incentive_base_amount", precision = 19, scale = 2)
    private BigDecimal incentiveBaseAmount; // Covered × 4%
    
    @Column(name = "target_achieved")
    private Boolean targetAchieved;
    
    @Column(name = "expense_within_limit")
    private Boolean expenseWithinLimit;
    
    @Column(name = "territory_eligible")
    private Boolean territoryEligible;
    
    @Column(name = "total_sr_share", precision = 19, scale = 2)
    private BigDecimal totalSrShare;
    
    @Column(name = "total_mpo_share", precision = 19, scale = 2)
    private BigDecimal totalMpoShare;
    
    @Column(name = "total_manager_share", precision = 19, scale = 2)
    private BigDecimal totalManagerShare;
    
    @Column(name = "total_incentive_distributed", precision = 19, scale = 2)
    private BigDecimal totalIncentiveDistributed;
    
    @Column(name = "calculation_date")
    private LocalDateTime calculationDate;
    
    @Column(name = "calculated_by")
    private UUID calculatedBy;
    
    @Column(name = "status", length = 50)
    private String status = "CALCULATED"; // CALCULATED, PAID
    
    @OneToMany(mappedBy = "incentiveCalculation", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<IncentiveDistribution> distributions;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

