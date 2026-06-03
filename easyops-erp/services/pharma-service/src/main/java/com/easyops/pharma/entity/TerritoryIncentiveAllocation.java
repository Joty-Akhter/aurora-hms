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
@Table(name = "territory_incentive_allocations", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TerritoryIncentiveAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "territory_incentive_rule_id", nullable = false)
    private UUID territoryIncentiveRuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_incentive_rule_id", insertable = false, updatable = false)
    private TerritoryIncentiveRule territoryIncentiveRule;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "role_in_territory", length = 50)
    private String roleInTerritory;

    @Column(name = "allocation_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal allocationPercentage;

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
