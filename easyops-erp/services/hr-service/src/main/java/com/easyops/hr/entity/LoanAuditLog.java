package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RE-04: Audit trail for loan principal, schedule changes, repayments, and settlement adjustments.
 */
@Entity
@Table(name = "loan_audit_log", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    public static final String ENTITY_LOAN = "LOAN";
    public static final String ENTITY_REPAYMENT = "REPAYMENT";
    public static final String ENTITY_INSTALLMENT_BULK = "INSTALLMENT_BULK";
    /** PI-05: org-scoped COA mapping for accounting export; {@code entity_id} is {@code organization_id}. */
    public static final String ENTITY_LOAN_ACCOUNTING_COA = "LOAN_ACCOUNTING_COA";
    /** AD-03 bulk: org-level operation; {@code entity_id} is {@code organization_id}. */
    public static final String ENTITY_LOAN_ORG = "LOAN_ORG";

    public static final String ACTION_DISBURSE = "DISBURSE";
    public static final String ACTION_REPAYMENT = "REPAYMENT";
    public static final String ACTION_SETTLEMENT_START = "SETTLEMENT_START";
    public static final String ACTION_SETTLEMENT_ALLOCATE = "SETTLEMENT_ALLOCATE";
    public static final String ACTION_SETTLEMENT_SHORTFALL = "SETTLEMENT_SHORTFALL";
    public static final String ACTION_SETTLEMENT_CLOSE = "SETTLEMENT_CLOSE";
    public static final String ACTION_SCHEDULE_CREATED = "SCHEDULE_CREATED";
    /** RP-05: payroll recovery reversed (pairs with original PAYROLL transaction). */
    public static final String ACTION_PAYROLL_REVERSAL = "PAYROLL_REVERSAL";
    /** RP-01: installment marked SKIPPED with reason. */
    public static final String ACTION_INSTALLMENT_SKIPPED = "INSTALLMENT_SKIPPED";
    /** ST-04: legal workflow label updated (not a full BPM). */
    public static final String ACTION_LEGAL_WORKFLOW_UPDATED = "LEGAL_WORKFLOW_UPDATED";
    /** AD-03: installment due dates recalculated from disbursement + holiday calendar. */
    public static final String ACTION_INSTALLMENT_DUE_DATES_RECALC = "INSTALLMENT_DUE_DATES_RECALC";
    /** PI-05: loan accounting COA mapping table replaced for the organization. */
    public static final String ACTION_COA_MAPPINGS_REPLACED = "COA_MAPPINGS_REPLACED";
    /** AD-03: bulk holiday recalculation finished for the organization (summary in {@code new_values}). */
    public static final String ACTION_BULK_HOLIDAY_RECALC_COMPLETED = "BULK_HOLIDAY_RECALC_COMPLETED";
}
