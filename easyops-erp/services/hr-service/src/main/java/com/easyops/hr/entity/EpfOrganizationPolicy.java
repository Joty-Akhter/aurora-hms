package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * INT-09 / INT-12: Organization-level EPF rates, PF wage ceiling/floor, and employment eligibility.
 */
@Entity
@Table(name = "epf_organization_policy", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpfOrganizationPolicy {

    @Id
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_contribution_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal employeeContributionRate = new BigDecimal("12.00");

    @Column(name = "employer_contribution_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal employerContributionRate = new BigDecimal("12.00");

    /** Max PF wage base per month; null = no ceiling. */
    @Column(name = "pf_wage_ceiling", precision = 14, scale = 2)
    private BigDecimal pfWageCeiling;

    /** Min PF wage to qualify for PF; null = no floor. */
    @Column(name = "pf_wage_floor", precision = 14, scale = 2)
    private BigDecimal pfWageFloor;

    /** Comma-separated (e.g. FULL_TIME,PART_TIME). Empty/null = all types eligible if not in ineligible list. */
    @Column(name = "eligible_employment_types", length = 500)
    private String eligibleEmploymentTypes;

    /** Comma-separated types excluded from PF (e.g. INTERN,CONTRACT). */
    @Column(name = "ineligible_employment_types", length = 500)
    private String ineligibleEmploymentTypes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
