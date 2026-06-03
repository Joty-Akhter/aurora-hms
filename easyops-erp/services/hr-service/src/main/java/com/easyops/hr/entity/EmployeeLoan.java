package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_loans", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmployeeLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "loan_id")
    private UUID loanId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "loan_application_id", unique = true)
    private UUID loanApplicationId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BDT";

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmployeeLoanStatus status;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "disbursed_amount", precision = 15, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(name = "settlement_shortfall_amount", precision = 15, scale = 2)
    private BigDecimal settlementShortfallAmount;

    @Column(name = "settlement_started_at")
    private LocalDateTime settlementStartedAt;

    @Column(name = "separation_effective_date")
    private LocalDate separationEffectiveDate;

    /** ST-04: configurable write-off / legal path label. */
    @Column(name = "settlement_write_off_path", length = 40)
    private String settlementWriteOffPath;

    @Column(name = "legal_case_reference", length = 200)
    private String legalCaseReference;

    @Column(name = "write_off_notes", columnDefinition = "TEXT")
    private String writeOffNotes;

    /** ST-04: lightweight state label — not a full BPM. */
    @Column(name = "legal_workflow_status", length = 40)
    private String legalWorkflowStatus;

    @Column(name = "legal_workflow_updated_at")
    private LocalDateTime legalWorkflowUpdatedAt;

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
