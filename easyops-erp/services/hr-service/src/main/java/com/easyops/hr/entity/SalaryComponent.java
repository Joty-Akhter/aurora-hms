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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "salary_components", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalaryComponent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "component_id")
    private UUID componentId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** SC-01, SC-06: Unique per organization; immutable after creation. */
    @Column(name = "code", nullable = false, length = 100, updatable = false)
    private String code;
    
    @Column(name = "component_name", nullable = false, length = 100)
    private String componentName;

    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "component_type", nullable = false, length = 50)
    private String componentType;

    /** SC-02: Category (e.g. Basic, HRA, Special Allowance, Statutory Deduction, Loan Repayment). */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 80)
    private SalaryComponentCategory category;
    
    /** SC-09: Calculation basis – FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS, FORMULA, STATUTORY, MANUAL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", length = 50)
    private CalculationBasis calculationBasis = CalculationBasis.FIXED;

    /** SC-10: Default amount for FIXED / optional for MANUAL. */
    @Column(name = "default_amount", precision = 14, scale = 2)
    private BigDecimal defaultAmount;

    /** SC-11: Percentage (0–100+) for PERCENTAGE_OF_BASIC / PERCENTAGE_OF_GROSS. */
    @Column(name = "percentage_value", precision = 8, scale = 2)
    private BigDecimal percentageValue;

    /** SC-11: Base component code (e.g. BASIC) for percentage-based; must be earning in same org. */
    @Column(name = "base_component_code", length = 100)
    private String baseComponentCode;

    /** SC-12: Formula expression e.g. BASIC * 0.4; validated for syntax and circular ref. */
    @Column(name = "formula_expression", length = 500)
    private String formulaExpression;

    /** SC-13: Statutory type e.g. PF_EMPLOYEE, INCOME_TAX for STATUTORY basis. */
    @Column(name = "statutory_type", length = 80)
    private String statutoryType;

    /** SC-16: Max amount; with floor, ceiling >= floor (SC-27). */
    @Column(name = "ceiling_amount", precision = 14, scale = 2)
    private BigDecimal ceilingAmount;

    /** SC-16: Min amount. */
    @Column(name = "floor_amount", precision = 14, scale = 2)
    private BigDecimal floorAmount;

    /** SC-17: Rounding rule e.g. ROUND_NEAREST_INTEGER, ROUND_UP, TWO_DECIMALS. */
    @Column(name = "rounding_rule", length = 50)
    private String roundingRule;

    /** SC-18 (optional): Conditional applicability rule (e.g. employee type, grade). */
    @Column(name = "applicability_rule", length = 500)
    private String applicabilityRule;
    
    @Column(name = "is_taxable")
    private Boolean isTaxable = true;

    /** SC-21: Taxability for income tax: TAXABLE, EXEMPT, PARTIALLY_TAXABLE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "taxability", length = 30)
    private Taxability taxability;

    /** SC-20: Statutory tags e.g. PF_WAGE, PF_EMPLOYEE, TAXABLE, TAX_EXEMPT, ESI_WAGE. PF wage = sum of components with PF_WAGE (SC-22). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "hr", name = "salary_component_statutory_tags", joinColumns = @JoinColumn(name = "component_id"))
    @Column(name = "tag", length = 50)
    private List<String> statutoryTags = new ArrayList<>();
    
    @Column(name = "is_statutory")
    private Boolean isStatutory = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** SC-04: Effective from date; component available for assignment/payroll on or after this date. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /** SC-04: Effective to date; null = open-ended. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** SC-05: Display order for payslips and reports (integer). */
    @Column(name = "display_order")
    private Integer displayOrder;

    /** SC-07: Optional short name / payslip label for display on payslips (e.g. "Basic Sal", "HRA"). If null, componentName is used. */
    @Column(name = "payslip_label", length = 100)
    private String shortName;

    /** SC-08: Optional currency for this component. When null, structure or organization default is used in payroll. */
    @Column(name = "currency", length = 3)
    private String currency;

    /** ES-29: Proration rule for partial period (mid-period join/leave): BY_DAYS, NO_PRORATION, BY_HOURS. Default per component. */
    @Enumerated(EnumType.STRING)
    @Column(name = "proration_rule", length = 30)
    private ProrationRule prorationRule = ProrationRule.BY_DAYS;

    /** INT-20: Optional COA code for salary expense when this component is an earning (payroll accounting export / journal mapping). */
    @Column(name = "expense_account_code", length = 64)
    private String expenseAccountCode;

    /** INT-20: Optional COA code for deduction/withholding when this component is a deduction. */
    @Column(name = "liability_account_code", length = 64)
    private String liabilityAccountCode;

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

