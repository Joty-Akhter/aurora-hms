package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_organization_settings", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanOrganizationSettings {

    @Id
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "min_tenure_months", nullable = false)
    private Integer minTenureMonths = 6;

    @Column(name = "max_principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxPrincipalAmount = new BigDecimal("150000.00");

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Column(name = "enforce_single_active_loan", nullable = false)
    private Boolean enforceSingleActiveLoan = true;

    @Column(name = "allow_salary_advance_with_active_term_loan", nullable = false)
    private Boolean allowSalaryAdvanceWithActiveTermLoan = false;

    /** JSON array of employment_status values that block loans (EL-02). */
    @Column(name = "disqualifying_employment_statuses", nullable = false, columnDefinition = "TEXT")
    private String disqualifyingEmploymentStatusesJson = "[\"LONG_TERM_SUSPENSION\",\"SUSPENDED\"]";

    /** JSON array: PF_SETTLEMENT, FINAL_SALARY, OTHER_DUES order (ST-03). */
    @Column(name = "settlement_allocation_priority", nullable = false, columnDefinition = "TEXT")
    private String settlementAllocationPriorityJson = "[\"PF_SETTLEMENT\",\"FINAL_SALARY\",\"OTHER_DUES\"]";

    @Column(name = "enforce_settlement_allocation_order", nullable = false)
    private Boolean enforceSettlementAllocationOrder = true;

    @Column(name = "skip_finance_approval", nullable = false)
    private Boolean skipFinanceApproval = false;

    /** BR-08: salary advance single-step (HR only) when true. */
    @Column(name = "salary_advance_skip_finance_approval", nullable = false)
    private Boolean salaryAdvanceSkipFinanceApproval = true;

    /** AD-03: shift installment due dates off weekends and {@code hr.holidays} when true. */
    @Column(name = "shift_installment_due_dates_for_holidays", nullable = false)
    private Boolean shiftInstallmentDueDatesForHolidays = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_holiday_shift_mode", nullable = false, length = 32)
    private LoanHolidayShiftMode loanHolidayShiftMode = LoanHolidayShiftMode.NEXT_BUSINESS_DAY;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
