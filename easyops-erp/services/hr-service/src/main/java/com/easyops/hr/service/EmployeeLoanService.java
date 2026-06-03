package com.easyops.hr.service;

import com.easyops.hr.dto.*;
import com.easyops.hr.entity.*;
import com.easyops.hr.integration.PfSettlementClient;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeLoanService implements ApprovedLoanCreator {

    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;
    private final LoanRepaymentAllocationRepository loanRepaymentAllocationRepository;
    private final LoanOrgSettingsProvider loanOrgSettingsProvider;
    private final LoanAuditService loanAuditService;
    private final PfSettlementClient pfSettlementClient;
    private final HolidayRepository holidayRepository;
    private final TransactionTemplate transactionTemplate;
    private final LoanCategoryRepository loanCategoryRepository;
    private final LoanNotificationService loanNotificationService;

    /**
     * Called when a loan application is approved: creates a loan shell and immediately disburses the full approved amount (AL-04, AL-05 full).
     */
    @Override
    @Transactional
    public void createLoanFromApprovedApplication(LoanApplication application) {
        if (employeeLoanRepository.findByLoanApplicationId(application.getApplicationId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan already exists for this application");
        }

        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(application.getOrganizationId());

        EmployeeLoan loan = new EmployeeLoan();
        loan.setOrganizationId(application.getOrganizationId());
        loan.setEmployeeId(application.getEmployeeId());
        loan.setLoanApplicationId(application.getApplicationId());
        loan.setCategoryId(application.getCategoryId());
        loan.setPrincipalAmount(application.getRequestedAmount());
        loan.setCurrency(settings.getCurrency());
        loan.setOutstandingBalance(application.getRequestedAmount());
        loan.setTenureMonths(application.getRequestedTenureMonths());
        loan.setStatus(EmployeeLoanStatus.PENDING_DISBURSEMENT);

        EmployeeLoan saved = employeeLoanRepository.save(loan);

        LoanDisbursementRequest disburse = new LoanDisbursementRequest();
        disburse.setAmount(saved.getPrincipalAmount());
        disburse.setDisbursementDate(LocalDate.now());
        disburseEmployeeLoan(application.getOrganizationId(), saved.getLoanId(), disburse, null);
    }

    /**
     * Disburse funds (full or partial). Generates installment schedule on the disbursed principal (AL-05).
     */
    @Transactional
    public EmployeeLoanDto disburseEmployeeLoan(UUID organizationId, UUID loanId, LoanDisbursementRequest request, UUID actorUserId) {
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.PENDING_DISBURSEMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan is not pending disbursement");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disbursement amount must be positive");
        }
        if (request.getAmount().compareTo(loan.getPrincipalAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disbursement cannot exceed approved principal");
        }

        BigDecimal disbursed = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        loan.setPrincipalAmount(disbursed);
        loan.setOutstandingBalance(disbursed);
        loan.setDisbursedAmount(disbursed);
        loan.setDisbursementDate(request.getDisbursementDate());
        loan.setStatus(EmployeeLoanStatus.ACTIVE);

        employeeLoanRepository.save(loan);

        LoanCategory category = loanCategoryRepository.findById(loan.getCategoryId()).orElse(null);
        LoanInterestMethod interestMethod =
                category != null && category.getInterestMethod() != null
                        ? category.getInterestMethod()
                        : LoanInterestMethod.NONE;
        BigDecimal categoryRate = category != null ? category.getFlatAnnualRatePercent() : null;
        List<LoanInstallment> schedule;
        try {
            schedule = buildInstallmentSchedule(
                    organizationId,
                    loan.getLoanId(),
                    disbursed,
                    loan.getTenureMonths(),
                    request.getDisbursementDate(),
                    interestMethod,
                    categoryRate);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        BigDecimal totalScheduled =
                schedule.stream().map(LoanInstallment::getScheduledAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        loan.setOutstandingBalance(totalScheduled.setScale(2, RoundingMode.HALF_UP));
        employeeLoanRepository.save(loan);
        loanInstallmentRepository.saveAll(schedule);

        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_DISBURSE, null,
                "disbursedPrincipal=" + disbursed + ",tenureMonths=" + loan.getTenureMonths(), actorUserId);
        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_SCHEDULE_CREATED, null,
                "installmentCount=" + schedule.size(), actorUserId);

        loanNotificationService.notifyLoanEmployee(
                organizationId,
                loan.getEmployeeId(),
                loanId,
                loan.getLoanApplicationId(),
                LoanNotificationService.EVT_LOAN_DISBURSED,
                "Loan disbursed",
                "Your loan of "
                        + disbursed.toPlainString()
                        + " "
                        + (loan.getCurrency() != null ? loan.getCurrency() : "")
                        + " has been disbursed. Repayment schedule has "
                        + schedule.size()
                        + " installments.");

        return getLoan(organizationId, loanId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLoanDto> listLoans(UUID organizationId, UUID employeeId, EmployeeLoanStatus status) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<EmployeeLoan> loans;
        if (employeeId != null) {
            loans = employeeLoanRepository.findByOrganizationIdAndEmployeeIdOrderByCreatedAtDesc(organizationId, employeeId);
        } else {
            loans = employeeLoanRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        }
        if (status != null) {
            loans = loans.stream().filter(l -> status.equals(l.getStatus())).toList();
        }
        return loans.stream().map(l -> toDto(l, false)).toList();
    }

    /** RE-02: self-service list with schedule rows. */
    @Transactional(readOnly = true)
    public List<EmployeeLoanDto> listLoansForEmployeeSelf(UUID organizationId, UUID employeeId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<EmployeeLoan> loans =
                employeeLoanRepository.findByOrganizationIdAndEmployeeIdOrderByCreatedAtDesc(organizationId, employeeId);
        return loans.stream().map(l -> toDto(l, true)).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeLoanDto getLoan(UUID organizationId, UUID loanId) {
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        return toDto(loan, true);
    }

    /** ST-04: update legal workflow state label (configurable strings; not a full task engine). */
    @Transactional
    public EmployeeLoanDto patchLegalWorkflow(
            UUID organizationId, UUID loanId, String legalWorkflowStatus, UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        String next = trimToNull(legalWorkflowStatus);
        loan.setLegalWorkflowStatus(next);
        loan.setLegalWorkflowUpdatedAt(LocalDateTime.now());
        employeeLoanRepository.save(loan);
        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_LEGAL_WORKFLOW_UPDATED, null,
                "legalWorkflowStatus=" + next, actorUserId);
        return getLoan(organizationId, loanId);
    }

    /**
     * Phase 5: move loan to settlement pending when employee exits with outstanding balance (ST-01).
     */
    @Transactional
    public EmployeeLoanDto startSettlement(
            UUID organizationId,
            UUID loanId,
            LoanSettlementStartRequest request,
            UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Settlement can only start for ACTIVE loans with outstanding balance");
        }
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No outstanding balance to settle");
        }
        loan.setStatus(EmployeeLoanStatus.SETTLEMENT_PENDING);
        loan.setSettlementStartedAt(LocalDateTime.now());
        loan.setSeparationEffectiveDate(request.getSeparationEffectiveDate());
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);
        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_SETTLEMENT_START, null,
                "separationEffectiveDate=" + request.getSeparationEffectiveDate(), actorUserId);

        loanNotificationService.notifyLoanEmployee(
                organizationId,
                loan.getEmployeeId(),
                loanId,
                loan.getLoanApplicationId(),
                LoanNotificationService.EVT_SETTLEMENT_REQUIRED,
                "Loan settlement required",
                "Your loan requires exit settlement. Effective date: "
                        + request.getSeparationEffectiveDate()
                        + ". Outstanding balance: "
                        + loan.getOutstandingBalance().toPlainString()
                        + ".");

        return getLoan(organizationId, loanId);
    }

    /**
     * Phase 5: allocate from PF, final salary, or other dues (BR-09, ST-02, ST-03, ST-05).
     */
    @Transactional
    public LoanManualRepaymentResultDto allocateSettlementRepayment(
            UUID organizationId,
            UUID loanId,
            LoanSettlementAllocateRequest request,
            UUID actorUserId) {
        if (!isExitSettlementSource(request.getSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "source must be PF_SETTLEMENT, FINAL_SALARY, or OTHER_DUES");
        }
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        validateSettlementAllocationOrder(organizationId, loan, request);
        String notes = buildSettlementNotes(request.getReference(), request.getNotes());
        return applyRepayment(
                organizationId,
                loanId,
                request.getAmount(),
                request.getPaymentDate(),
                request.getSource(),
                notes,
                null,
                actorUserId);
    }

    /** ST-03: enforce PF before other sources when PF funds are available and org policy enabled. */
    private void validateSettlementAllocationOrder(
            UUID organizationId, EmployeeLoan loan, LoanSettlementAllocateRequest request) {
        LoanOrganizationSettingsDto st = loanOrgSettingsProvider.getSettings(organizationId);
        if (!Boolean.TRUE.equals(st.getEnforceSettlementAllocationOrder())) {
            return;
        }
        if (request.getSource() == LoanRepaymentSource.PF_SETTLEMENT) {
            return;
        }
        Optional<BigDecimal> pfHint = pfSettlementClient.getAvailableSettlementAmount(organizationId, loan.getEmployeeId());
        if (pfHint.isEmpty() || pfHint.get().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal pfAllocated =
                loanRepaymentTransactionRepository.sumAmountByLoanIdAndSource(loan.getLoanId(), LoanRepaymentSource.PF_SETTLEMENT);
        if (pfAllocated.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Per organization policy, allocate PF settlement before other sources (PF available hint: " + pfHint.get() + ")");
        }
    }

    /**
     * ST-04: record remaining debt after allocations (audit); does not change outstanding balance.
     */
    @Transactional
    public EmployeeLoanDto recordSettlementShortfall(
            UUID organizationId,
            UUID loanId,
            LoanSettlementShortfallRequest request,
            UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.SETTLEMENT_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Shortfall can only be recorded for loans in SETTLEMENT_PENDING");
        }
        BigDecimal amt = request.getShortfallAmount().setScale(2, RoundingMode.HALF_UP);
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shortfallAmount must be positive");
        }
        if (amt.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "shortfallAmount cannot exceed outstanding balance");
        }
        loan.setSettlementShortfallAmount(amt);
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);
        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_SETTLEMENT_SHORTFALL, null,
                "shortfallAmount=" + amt + ",outstanding=" + loan.getOutstandingBalance(), actorUserId);
        return getLoan(organizationId, loanId);
    }

    /**
     * Close loan after settlement: when balance is zero from allocations, or write off remainder (ST-04, ST-05).
     */
    @Transactional
    public EmployeeLoanDto closeSettlementLoan(
            UUID organizationId,
            UUID loanId,
            LoanSettlementCloseRequest request,
            UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.SETTLEMENT_PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Loan must be in SETTLEMENT_PENDING to close via settlement");
        }
        BigDecimal os = loan.getOutstandingBalance();
        if (os.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(EmployeeLoanStatus.CLOSED);
            loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
            employeeLoanRepository.save(loan);
            loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_SETTLEMENT_CLOSE, null,
                    "outstanding=0", actorUserId);
            return getLoan(organizationId, loanId);
        }
        if (!Boolean.TRUE.equals(request.getWriteOffRemaining())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outstanding balance remains; post allocations or set writeOffRemaining to close with write-off");
        }
        loan.setSettlementShortfallAmount(os.setScale(2, RoundingMode.HALF_UP));
        loan.setSettlementWriteOffPath(trimToNull(request.getSettlementWriteOffPath()));
        loan.setLegalCaseReference(trimToNull(request.getLegalCaseReference()));
        String woNotes = trimToNull(request.getWriteOffNotes());
        if (woNotes == null) {
            woNotes = trimToNull(request.getReason());
        }
        loan.setWriteOffNotes(woNotes);
        waiveOutstandingInstallments(loan.getLoanId());
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setStatus(EmployeeLoanStatus.CLOSED);
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);
        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_SETTLEMENT_CLOSE, null,
                "writeOff=true,shortfallRecorded=" + loan.getSettlementShortfallAmount()
                        + ",path=" + loan.getSettlementWriteOffPath()
                        + ",legalRef=" + loan.getLegalCaseReference(),
                actorUserId);
        return getLoan(organizationId, loanId);
    }

    /**
     * Manual repayment: FIFO allocation across installments (RP-02, RP-03); supports partial and multi-installment payments.
     */
    @Transactional
    public LoanManualRepaymentResultDto recordManualRepayment(
            UUID organizationId,
            UUID loanId,
            LoanManualRepaymentRequest request,
            UUID actorUserId) {
        return applyRepayment(
                organizationId,
                loanId,
                request.getAmount(),
                request.getPaymentDate(),
                LoanRepaymentSource.MANUAL,
                trimToNull(request.getNotes()),
                null,
                actorUserId);
    }

    /**
     * Payroll deduction posting (RP-04, PI-01–PI-04). Idempotent per (loanId, payrollRunId).
     */
    @Transactional
    public LoanManualRepaymentResultDto recordPayrollRepayment(
            UUID organizationId,
            UUID loanId,
            BigDecimal amount,
            LocalDate paymentDate,
            UUID payrollRunId,
            UUID actorUserId) {
        if (loanRepaymentTransactionRepository.existsByLoanIdAndPayrollRunId(loanId, payrollRunId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Loan repayment already posted for this payroll run (idempotent key)");
        }
        return applyRepayment(
                organizationId,
                loanId,
                amount,
                paymentDate,
                LoanRepaymentSource.PAYROLL,
                "Payroll deduction",
                payrollRunId,
                actorUserId);
    }

    /**
     * RP-05: reverse a posted PAYROLL transaction (restores installment paid amounts and outstanding; audit).
     */
    @Transactional
    public LoanManualRepaymentResultDto reversePayrollRepayment(
            UUID organizationId,
            UUID loanId,
            UUID originalTransactionId,
            LoanPayrollRepaymentReversalRequest request,
            UUID actorUserId) {
        LoanRepaymentTransaction orig = loanRepaymentTransactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment transaction not found"));
        if (!loanId.equals(orig.getLoanId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction does not belong to this loan");
        }
        if (orig.getSource() != LoanRepaymentSource.PAYROLL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PAYROLL postings can be reversed this way");
        }
        if (loanRepaymentTransactionRepository.existsByReversesTransactionId(originalTransactionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payroll posting was already reversed");
        }

        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.ACTIVE && loan.getStatus() != EmployeeLoanStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payroll reversal is only supported for ACTIVE or CLOSED loans");
        }

        List<LoanRepaymentAllocation> allocs =
                loanRepaymentAllocationRepository.findByTransactionIdOrderByAllocationId(originalTransactionId);
        if (allocs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Original transaction has no allocations to reverse");
        }

        for (LoanRepaymentAllocation a : allocs) {
            LoanInstallment inst = loanInstallmentRepository.findById(a.getInstallmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Installment missing"));
            if (!loanId.equals(inst.getLoanId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Installment does not belong to loan");
            }
            BigDecimal newPaid = inst.getPaidAmount().subtract(a.getAmount()).setScale(2, RoundingMode.HALF_UP);
            if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reverse: installment paid amount would be negative");
            }
            inst.setPaidAmount(newPaid);
            inst.setStatus(resolveInstallmentStatus(inst));
            loanInstallmentRepository.save(inst);
        }

        List<LoanInstallment> allInst = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        BigDecimal newOutstanding = computeOutstandingFromInstallments(allInst);
        loan.setOutstandingBalance(newOutstanding);
        if (newOutstanding.compareTo(BigDecimal.ZERO) > 0 && loan.getStatus() == EmployeeLoanStatus.CLOSED) {
            loan.setStatus(EmployeeLoanStatus.ACTIVE);
        }
        if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus(EmployeeLoanStatus.CLOSED);
        }
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);

        LoanRepaymentTransaction rev = new LoanRepaymentTransaction();
        rev.setLoanId(loanId);
        rev.setAmount(orig.getAmount().negate().setScale(2, RoundingMode.HALF_UP));
        rev.setPaymentDate(orig.getPaymentDate());
        rev.setSource(LoanRepaymentSource.PAYROLL_REVERSAL);
        rev.setNotes(trimToNull(request.getReason()));
        rev.setPayrollRunId(orig.getPayrollRunId());
        rev.setReversesTransactionId(orig.getTransactionId());
        rev.setCreatedBy(actorUserId != null ? actorUserId.toString() : null);
        LoanRepaymentTransaction savedRev = loanRepaymentTransactionRepository.save(rev);

        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_PAYROLL_REVERSAL, null,
                "reversalTransactionId=" + savedRev.getTransactionId() + ",reverses=" + originalTransactionId
                        + ",amount=" + orig.getAmount() + ",newOutstanding=" + loan.getOutstandingBalance(),
                actorUserId);

        return LoanManualRepaymentResultDto.builder()
                .transactionId(savedRev.getTransactionId())
                .loanId(loanId)
                .totalAmount(orig.getAmount())
                .newOutstandingBalance(loan.getOutstandingBalance())
                .loanClosed(loan.getStatus() == EmployeeLoanStatus.CLOSED)
                .allocations(List.of())
                .build();
    }

    private LoanManualRepaymentResultDto applyRepayment(
            UUID organizationId,
            UUID loanId,
            BigDecimal amount,
            LocalDate paymentDate,
            LoanRepaymentSource source,
            String notes,
            UUID payrollRunId,
            UUID actorUserId) {

        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (isExitSettlementSource(source)) {
            if (loan.getStatus() != EmployeeLoanStatus.SETTLEMENT_PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Exit settlement allocations require loan status SETTLEMENT_PENDING");
            }
        } else if (loan.getStatus() != EmployeeLoanStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repayments are only allowed on ACTIVE loans");
        }

        BigDecimal payment = amount.setScale(2, RoundingMode.HALF_UP);
        if (payment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repayment amount must be positive");
        }
        if (payment.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repayment exceeds outstanding balance");
        }

        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        BigDecimal remaining = payment;
        List<LoanRepaymentAllocationDto> allocationDtos = new ArrayList<>();

        for (LoanInstallment inst : installments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal dueOnInst = inst.getScheduledAmount().subtract(inst.getPaidAmount());
            if (dueOnInst.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal apply = remaining.min(dueOnInst);
            inst.setPaidAmount(inst.getPaidAmount().add(apply).setScale(2, RoundingMode.HALF_UP));
            if (inst.getPaidAmount().compareTo(inst.getScheduledAmount()) > 0) {
                inst.setPaidAmount(inst.getScheduledAmount());
            }
            inst.setStatus(resolveInstallmentStatus(inst));
            remaining = remaining.subtract(apply);
            allocationDtos.add(LoanRepaymentAllocationDto.builder()
                    .installmentId(inst.getInstallmentId())
                    .sequenceNumber(inst.getSequenceNumber())
                    .allocatedAmount(apply)
                    .build());
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not allocate repayment (check installment state)");
        }

        LoanRepaymentTransaction repayment = new LoanRepaymentTransaction();
        repayment.setLoanId(loanId);
        repayment.setAmount(payment);
        repayment.setPaymentDate(paymentDate);
        repayment.setSource(source);
        repayment.setNotes(trimToNull(notes));
        repayment.setPayrollRunId(payrollRunId);
        repayment.setCreatedBy(actorUserId != null ? actorUserId.toString() : null);
        LoanRepaymentTransaction savedRepayment = loanRepaymentTransactionRepository.save(repayment);

        List<LoanRepaymentAllocation> allocs = new ArrayList<>();
        for (LoanRepaymentAllocationDto ad : allocationDtos) {
            LoanRepaymentAllocation a = new LoanRepaymentAllocation();
            a.setTransactionId(savedRepayment.getTransactionId());
            a.setInstallmentId(ad.getInstallmentId());
            a.setAmount(ad.getAllocatedAmount());
            allocs.add(a);
        }
        loanRepaymentAllocationRepository.saveAll(allocs);

        loanInstallmentRepository.saveAll(installments);

        BigDecimal newOutstanding = computeOutstandingFromInstallments(installments);
        loan.setOutstandingBalance(newOutstanding);
        if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus(EmployeeLoanStatus.CLOSED);
        }
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);

        String auditAction = isExitSettlementSource(source)
                ? LoanAuditLog.ACTION_SETTLEMENT_ALLOCATE
                : LoanAuditLog.ACTION_REPAYMENT;
        loanAuditService.logLoan(organizationId, loanId, auditAction, null,
                "transactionId=" + savedRepayment.getTransactionId() + ",source=" + source + ",amount=" + payment
                        + ",newOutstanding=" + loan.getOutstandingBalance(),
                actorUserId);

        return LoanManualRepaymentResultDto.builder()
                .transactionId(savedRepayment.getTransactionId())
                .loanId(loanId)
                .totalAmount(payment)
                .newOutstandingBalance(loan.getOutstandingBalance())
                .loanClosed(loan.getStatus() == EmployeeLoanStatus.CLOSED)
                .allocations(allocationDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentTransaction> listRepaymentTransactions(UUID organizationId, UUID loanId) {
        loadLoan(organizationId, loanId);
        return loanRepaymentTransactionRepository.findByLoanIdOrderByCreatedAtDesc(loanId);
    }

    /**
     * RP-01: administratively skip an installment (waive remaining due for this period) with reason; audited.
     */
    @Transactional
    public LoanInstallmentDto skipInstallment(
            UUID organizationId,
            UUID loanId,
            UUID installmentId,
            LoanInstallmentSkipRequest request,
            UUID actorUserId) {
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getStatus() != EmployeeLoanStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Installment skip is only allowed for ACTIVE loans");
        }
        LoanInstallment inst = loanInstallmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installment not found"));
        if (!loanId.equals(inst.getLoanId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Installment does not belong to this loan");
        }
        if (inst.getStatus() == LoanInstallmentStatus.SKIPPED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Installment is already skipped");
        }
        BigDecimal due = inst.getScheduledAmount().subtract(inst.getPaidAmount());
        if (due.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No remaining amount to skip on this installment");
        }
        String reason = trimToNull(request.getReason());
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason is required");
        }
        inst.setPaidAmount(inst.getScheduledAmount());
        inst.setStatus(LoanInstallmentStatus.SKIPPED);
        inst.setSkipReason(reason);
        loanInstallmentRepository.save(inst);

        List<LoanInstallment> all = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        BigDecimal newOs = computeOutstandingFromInstallments(all);
        loan.setOutstandingBalance(newOs);
        if (newOs.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus(EmployeeLoanStatus.CLOSED);
        }
        loan.setUpdatedBy(actorUserId != null ? actorUserId.toString() : null);
        employeeLoanRepository.save(loan);

        loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_INSTALLMENT_SKIPPED, null,
                "installmentId=" + installmentId + ",sequence=" + inst.getSequenceNumber() + ",reason=" + reason,
                actorUserId);

        return toInstallmentDto(inst, LocalDate.now());
    }

    private void waiveOutstandingInstallments(UUID loanId) {
        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        for (LoanInstallment inst : installments) {
            BigDecimal dueOnInst = inst.getScheduledAmount().subtract(inst.getPaidAmount());
            if (dueOnInst.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            inst.setPaidAmount(inst.getScheduledAmount());
            inst.setStatus(LoanInstallmentStatus.PAID);
        }
        loanInstallmentRepository.saveAll(installments);
    }

    private static boolean isExitSettlementSource(LoanRepaymentSource source) {
        return source == LoanRepaymentSource.PF_SETTLEMENT
                || source == LoanRepaymentSource.FINAL_SALARY
                || source == LoanRepaymentSource.OTHER_DUES;
    }

    private static String buildSettlementNotes(String reference, String notes) {
        StringBuilder sb = new StringBuilder();
        if (reference != null && !reference.isBlank()) {
            sb.append("Ref: ").append(reference.trim());
        }
        if (notes != null && !notes.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(notes.trim());
        }
        String s = sb.toString();
        if (s.length() > 2000) {
            return s.substring(0, 2000);
        }
        return s.isEmpty() ? "Settlement allocation" : s;
    }

    private EmployeeLoan loadLoan(UUID organizationId, UUID loanId) {
        return employeeLoanRepository.findByLoanIdAndOrganizationId(loanId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
    }

    @Transactional
    public EmployeeLoanDto recalculateInstallmentDueDatesForHolidays(
            UUID organizationId, UUID loanId, UUID actorUserId) {
        return doRecalculateInstallmentDueDatesForHolidays(organizationId, loanId, actorUserId, true);
    }

    /**
     * AD-03: re-apply holiday rules to all active / settlement-pending loans with a disbursement date.
     * Each loan runs in its own transaction; failures are collected without aborting the rest.
     */
    public LoanBulkHolidayRecalcResultDto recalculateInstallmentDueDatesForHolidaysAll(
            UUID organizationId, UUID actorUserId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<EmployeeLoan> loans = employeeLoanRepository.findByOrganizationIdAndStatusIn(
                organizationId, EnumSet.of(EmployeeLoanStatus.ACTIVE, EmployeeLoanStatus.SETTLEMENT_PENDING));
        int recalculated = 0;
        int skipped = 0;
        List<LoanBulkHolidayRecalcFailureDto> failures = new ArrayList<>();
        for (EmployeeLoan loan : loans) {
            if (loan.getDisbursementDate() == null) {
                skipped++;
                continue;
            }
            try {
                transactionTemplate.executeWithoutResult(status ->
                        doRecalculateInstallmentDueDatesForHolidays(organizationId, loan.getLoanId(), actorUserId, false));
                recalculated++;
            } catch (Exception e) {
                failures.add(LoanBulkHolidayRecalcFailureDto.builder()
                        .loanId(loan.getLoanId())
                        .message(bulkHolidayRecalcFailureMessage(e))
                        .build());
            }
        }
        String summary = "recalculated=" + recalculated + ",skippedNoDisbursement=" + skipped + ",failed=" + failures.size();
        loanAuditService.log(
                organizationId,
                LoanAuditLog.ENTITY_LOAN_ORG,
                organizationId,
                LoanAuditLog.ACTION_BULK_HOLIDAY_RECALC_COMPLETED,
                null,
                summary,
                actorUserId);
        return LoanBulkHolidayRecalcResultDto.builder()
                .organizationId(organizationId)
                .loansRecalculated(recalculated)
                .loansSkipped(skipped)
                .failures(failures)
                .build();
    }

    /**
     * AD-03: recompute due dates for non-skipped, not-fully-paid installments from disbursement + sequence.
     * When {@code shiftInstallmentDueDatesForHolidays} is false, {@link LoanInstallmentDueDateAdjuster} still runs with
     * shifting off, so each row becomes the nominal {@code disbursement.plusMonths(sequence)}—re-baselining the schedule.
     * That is acceptable until manual per-installment due-date edits exist; then product rules may need a flag or to skip
     * adjusted rows (see requirements §13.5).
     *
     * @param writeLoanAudit when false (bulk), only persistence runs; org-level bulk audit is logged separately.
     */
    private EmployeeLoanDto doRecalculateInstallmentDueDatesForHolidays(
            UUID organizationId, UUID loanId, UUID actorUserId, boolean writeLoanAudit) {
        loanOrgSettingsProvider.getSettings(organizationId);
        EmployeeLoan loan = loadLoan(organizationId, loanId);
        if (loan.getDisbursementDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan has no disbursement date");
        }
        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(organizationId);
        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        LocalDate rangeEnd = loan.getDisbursementDate().plusMonths(loan.getTenureMonths() + 2);
        List<Holiday> holidays = holidayRepository.findOrgWideActiveHolidaysInRange(
                organizationId, loan.getDisbursementDate(), rangeEnd);
        Set<LocalDate> holidayDates = holidays.stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
        boolean shift = Boolean.TRUE.equals(settings.getShiftInstallmentDueDatesForHolidays());
        LoanHolidayShiftMode mode = settings.getLoanHolidayShiftMode();
        for (LoanInstallment i : installments) {
            if (i.getStatus() == LoanInstallmentStatus.SKIPPED) {
                continue;
            }
            if (isInstallmentFullyPaid(i)) {
                continue;
            }
            LocalDate raw = loan.getDisbursementDate().plusMonths(i.getSequenceNumber());
            LocalDate adj = safeAdjustDueDate(raw, shift, mode, holidayDates);
            i.setDueDate(adj);
        }
        loanInstallmentRepository.saveAll(installments);
        if (writeLoanAudit) {
            loanAuditService.logLoan(organizationId, loanId, LoanAuditLog.ACTION_INSTALLMENT_DUE_DATES_RECALC, null,
                    "AD-03", actorUserId);
        }
        return getLoan(organizationId, loanId);
    }

    private static boolean isInstallmentFullyPaid(LoanInstallment i) {
        BigDecimal paid = i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal sched = i.getScheduledAmount() != null ? i.getScheduledAmount() : BigDecimal.ZERO;
        return paid.compareTo(sched) >= 0;
    }

    private static LocalDate safeAdjustDueDate(
            LocalDate raw, boolean shift, LoanHolidayShiftMode mode, Set<LocalDate> holidayDates) {
        try {
            return LoanInstallmentDueDateAdjuster.adjust(raw, shift, mode, holidayDates);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    private static String bulkHolidayRecalcFailureMessage(Exception e) {
        if (e instanceof ResponseStatusException rse) {
            if (rse.getReason() != null && !rse.getReason().isBlank()) {
                return rse.getReason();
            }
            if (rse.getMessage() != null && !rse.getMessage().isBlank()) {
                return rse.getMessage();
            }
            return String.valueOf(rse.getStatusCode().value());
        }
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        Throwable c = e.getCause();
        if (c != null && c.getMessage() != null && !c.getMessage().isBlank()) {
            return c.getClass().getSimpleName() + ": " + c.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    /** Used by tests (reflection) and legacy callers; equal-principal / zero-interest. */
    private List<LoanInstallment> buildEqualPrincipalSchedule(
            UUID organizationId,
            UUID loanId,
            BigDecimal principal,
            int tenureMonths,
            LocalDate disbursementDate) {
        return buildInstallmentSchedule(
                organizationId, loanId, principal, tenureMonths, disbursementDate, LoanInterestMethod.NONE, null);
    }

    private List<LoanInstallment> buildInstallmentSchedule(
            UUID organizationId,
            UUID loanId,
            BigDecimal principal,
            int tenureMonths,
            LocalDate disbursementDate,
            LoanInterestMethod interestMethod,
            BigDecimal flatAnnualRatePercent) {
        if (tenureMonths <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenure must be positive");
        }
        List<BigDecimal> amounts;
        try {
            amounts = LoanScheduleBuilder.monthlyScheduledAmounts(principal, tenureMonths, interestMethod, flatAnnualRatePercent);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(organizationId);
        LocalDate rangeEnd = disbursementDate.plusMonths(tenureMonths + 2);
        List<Holiday> holidays = holidayRepository.findOrgWideActiveHolidaysInRange(organizationId, disbursementDate, rangeEnd);
        Set<LocalDate> holidayDates = holidays.stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsActive()))
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
        boolean shift = Boolean.TRUE.equals(settings.getShiftInstallmentDueDatesForHolidays());
        LoanHolidayShiftMode mode = settings.getLoanHolidayShiftMode();

        List<LoanInstallment> list = new ArrayList<>();
        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal amt = amounts.get(i - 1);
            LocalDate rawDue = disbursementDate.plusMonths(i);
            LocalDate dueDate = safeAdjustDueDate(rawDue, shift, mode, holidayDates);

            LoanInstallment inst = new LoanInstallment();
            inst.setLoanId(loanId);
            inst.setSequenceNumber(i);
            inst.setDueDate(dueDate);
            inst.setScheduledAmount(amt);
            inst.setPaidAmount(BigDecimal.ZERO);
            inst.setStatus(LoanInstallmentStatus.DUE);
            list.add(inst);
        }
        return list;
    }

    private static LoanInstallmentStatus resolveInstallmentStatus(LoanInstallment inst) {
        if (inst.getStatus() == LoanInstallmentStatus.SKIPPED) {
            return LoanInstallmentStatus.SKIPPED;
        }
        int cmp = inst.getPaidAmount().compareTo(inst.getScheduledAmount());
        if (cmp >= 0) {
            return LoanInstallmentStatus.PAID;
        }
        if (inst.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            return LoanInstallmentStatus.PARTIAL;
        }
        return LoanInstallmentStatus.DUE;
    }

    private static BigDecimal computeOutstandingFromInstallments(List<LoanInstallment> installments) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LoanInstallment i : installments) {
            BigDecimal rem = i.getScheduledAmount().subtract(i.getPaidAmount());
            if (rem.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(rem);
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private EmployeeLoanDto toDto(EmployeeLoan loan, boolean includeInstallments) {
        List<LoanInstallmentDto> instDtos = null;
        if (includeInstallments) {
            List<LoanInstallment> rows = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loan.getLoanId());
            LocalDate today = LocalDate.now();
            instDtos = rows.stream().map(r -> toInstallmentDto(r, today)).toList();
        }
        return EmployeeLoanDto.builder()
                .loanId(loan.getLoanId())
                .organizationId(loan.getOrganizationId())
                .employeeId(loan.getEmployeeId())
                .loanApplicationId(loan.getLoanApplicationId())
                .categoryId(loan.getCategoryId())
                .principalAmount(loan.getPrincipalAmount())
                .currency(loan.getCurrency())
                .outstandingBalance(loan.getOutstandingBalance())
                .tenureMonths(loan.getTenureMonths())
                .status(loan.getStatus())
                .disbursementDate(loan.getDisbursementDate())
                .disbursedAmount(loan.getDisbursedAmount())
                .settlementShortfallAmount(loan.getSettlementShortfallAmount())
                .settlementStartedAt(loan.getSettlementStartedAt())
                .separationEffectiveDate(loan.getSeparationEffectiveDate())
                .settlementWriteOffPath(loan.getSettlementWriteOffPath())
                .legalCaseReference(loan.getLegalCaseReference())
                .writeOffNotes(loan.getWriteOffNotes())
                .legalWorkflowStatus(loan.getLegalWorkflowStatus())
                .legalWorkflowUpdatedAt(loan.getLegalWorkflowUpdatedAt())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .installments(instDtos)
                .build();
    }

    private LoanInstallmentDto toInstallmentDto(LoanInstallment r, LocalDate today) {
        BigDecimal remaining = r.getScheduledAmount().subtract(r.getPaidAmount()).max(BigDecimal.ZERO);
        boolean overdue = remaining.compareTo(BigDecimal.ZERO) > 0
                && r.getDueDate().isBefore(today);
        return LoanInstallmentDto.builder()
                .installmentId(r.getInstallmentId())
                .sequenceNumber(r.getSequenceNumber())
                .dueDate(r.getDueDate())
                .scheduledAmount(r.getScheduledAmount())
                .paidAmount(r.getPaidAmount())
                .remainingAmount(remaining.setScale(2, RoundingMode.HALF_UP))
                .status(r.getStatus())
                .skipReason(r.getSkipReason())
                .overdue(overdue)
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
