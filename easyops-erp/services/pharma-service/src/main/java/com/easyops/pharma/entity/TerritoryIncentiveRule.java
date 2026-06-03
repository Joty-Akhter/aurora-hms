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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "territory_incentive_rules", schema = "pharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TerritoryIncentiveRule implements Serializable {

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

    @Column(name = "incentive_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal incentivePercentage = new BigDecimal("0.0400");

    @Column(name = "sr_share_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal srSharePercentage = new BigDecimal("0.0900");

    @Column(name = "development_fund_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal developmentFundPercentage = new BigDecimal("0.0100");

    @Column(name = "has_dedicated_sr", nullable = false)
    private Boolean hasDedicatedSr = true;

    @Column(name = "dual_role_employee_id")
    private UUID dualRoleEmployeeId;

    /** @deprecated Kept for backward compatibility; use territory_incentive_allocations for per-employee % */
    @Deprecated
    @Column(name = "mpo_share_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal mpoSharePercentage = new BigDecimal("0.7200");

    /** @deprecated Kept for backward compatibility; use territory_incentive_allocations for per-employee % */
    @Deprecated
    @Column(name = "manager_share_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal managerSharePercentage = new BigDecimal("0.1800");

    @Column(name = "expense_limit_percentage", precision = 5, scale = 4, nullable = false)
    private BigDecimal expenseLimitPercentage = new BigDecimal("0.3000");

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion = 1;

    @Column(name = "effective_from_date")
    private LocalDate effectiveFromDate;

    @Column(name = "effective_to_date")
    private LocalDate effectiveToDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
