package com.easyops.hr.service;

import com.easyops.hr.dto.LoanRecoveryLineDto;
import com.easyops.hr.dto.LoanOrganizationSettingsDto;
import com.easyops.hr.integration.IntegrationCorrelationIdHolder;
import com.easyops.hr.dto.LoanRepaymentAnomalyDto;
import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanInstallment;
import com.easyops.hr.entity.LoanRepaymentSource;
import com.easyops.hr.entity.LoanRepaymentTransaction;
import com.easyops.hr.entity.PayrollComponent;
import com.easyops.hr.entity.PayrollDetail;
import com.easyops.hr.entity.PayrollRun;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryComponentCategory;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.LoanInstallmentRepository;
import com.easyops.hr.repository.LoanRepaymentTransactionRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollDetailRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanPayrollRecoveryService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2;
    private static final BigDecimal VARIANCE_TOLERANCE = new BigDecimal("0.02");

    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final PayrollLoanRepaymentRecorder payrollLoanRepaymentRecorder;
    private final LoanOrgSettingsProvider loanOrgSettingsProvider;

    /**
     * Recovery due through {@code periodEnd} (arrears + installments due on or before period end), per active loan.
     * When {@code payrollRunId} is set, amounts are zero if already posted for that run (PI-03 / idempotency preview).
     */
    @Transactional(readOnly = true)
    public List<LoanRecoveryLineDto> getRecoveriesForPayroll(
            UUID organizationId,
            LocalDate periodStart,
            LocalDate periodEnd,
            UUID payrollRunId) {

        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodEnd cannot be before periodStart");
        }

        LoanOrganizationSettingsDto settings = loanOrgSettingsProvider.getSettings(organizationId);
        LocalDate end = periodEnd != null ? periodEnd : periodStart;
        if (end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodEnd or periodStart is required");
        }

        List<EmployeeLoan> activeLoans = employeeLoanRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                organizationId, EmployeeLoanStatus.ACTIVE);

        List<LoanRecoveryLineDto> lines = new ArrayList<>();
        for (EmployeeLoan loan : activeLoans) {
            boolean alreadyPosted = payrollRunId != null
                    && loanRepaymentTransactionRepository.existsByLoanIdAndPayrollRunId(loan.getLoanId(), payrollRunId);
            if (alreadyPosted) {
                lines.add(LoanRecoveryLineDto.builder()
                        .loanId(loan.getLoanId())
                        .employeeId(loan.getEmployeeId())
                        .amount(BigDecimal.ZERO.setScale(SCALE, ROUNDING))
                        .currency(loan.getCurrency())
                        .alreadyPostedForRun(true)
                        .build());
                continue;
            }

            BigDecimal recovery = computeRecoveryDueThrough(loan.getLoanId(), end);
            recovery = recovery.min(loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO);
            recovery = recovery.setScale(SCALE, ROUNDING);

            if (recovery.compareTo(BigDecimal.ZERO) < 1) {
                continue;
            }

            lines.add(LoanRecoveryLineDto.builder()
                    .loanId(loan.getLoanId())
                    .employeeId(loan.getEmployeeId())
                    .amount(recovery)
                    .currency(loan.getCurrency() != null ? loan.getCurrency() : settings.getCurrency())
                    .alreadyPostedForRun(false)
                    .build());
        }
        return lines;
    }

    /**
     * Recovery for one employee (for payroll calculation / payslip). Zero if no active loan or already posted for run.
     */
    @Transactional(readOnly = true)
    public BigDecimal getRecoveryAmountForEmployee(
            UUID organizationId,
            UUID employeeId,
            LocalDate periodEnd,
            UUID payrollRunId) {

        if (periodEnd == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        return employeeLoanRepository
                .findByOrganizationIdAndEmployeeIdAndStatus(organizationId, employeeId, EmployeeLoanStatus.ACTIVE)
                .map(loan -> {
                    if (payrollRunId != null
                            && loanRepaymentTransactionRepository.existsByLoanIdAndPayrollRunId(loan.getLoanId(), payrollRunId)) {
                        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                    }
                    BigDecimal recovery = computeRecoveryDueThrough(loan.getLoanId(), periodEnd);
                    recovery = recovery.min(loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO);
                    return recovery.setScale(SCALE, ROUNDING);
                })
                .orElse(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
    }

    /**
     * Posts PAYROLL repayments for all loans with positive recovery for the run's period.
     * Idempotent: skips loans already posted for {@code payrollRunId}.
     */
    /**
     * RP-05: surface payroll recovery reversals for review (alerts). Idempotent posting is unchanged; this lists controlled reversals.
     */
    @Transactional(readOnly = true)
    public List<LoanRepaymentAnomalyDto> listPayrollRecoveryAnomalies(UUID organizationId, LocalDateTime since) {
        loanOrgSettingsProvider.getSettings(organizationId);
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(90);
        List<LoanRepaymentTransaction> rows = loanRepaymentTransactionRepository.findPayrollReversalsSince(
                organizationId, LoanRepaymentSource.PAYROLL_REVERSAL, effectiveSince);
        List<LoanRepaymentAnomalyDto> out = new ArrayList<>();
        for (LoanRepaymentTransaction t : rows) {
            EmployeeLoan loan = employeeLoanRepository.findByLoanIdAndOrganizationId(t.getLoanId(), organizationId)
                    .orElse(null);
            if (loan == null) {
                continue;
            }
            out.add(LoanRepaymentAnomalyDto.builder()
                    .type(LoanRepaymentAnomalyDto.AnomalyType.PAYROLL_REVERSAL_RECORDED)
                    .loanId(loan.getLoanId())
                    .employeeId(loan.getEmployeeId())
                    .transactionId(t.getTransactionId())
                    .message("Payroll recovery reversed (controlled). reversesTransactionId=" + t.getReversesTransactionId())
                    .detectedAt(t.getCreatedAt())
                    .build());
        }
        return out;
    }

    /**
     * RP-05: compare payslip loan-component lines to loan-module PAYROLL postings; flag finalized runs with expected recovery but no posting.
     */
    @Transactional(readOnly = true)
    public List<LoanRepaymentAnomalyDto> listPayrollCrossCheck(UUID organizationId, UUID payrollRunId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));
        if (!organizationId.equals(run.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run does not belong to organization");
        }
        List<LoanRepaymentAnomalyDto> out = new ArrayList<>();
        out.addAll(comparePayslipToLoanPostings(organizationId, run));
        if (isPayrollRunFinalized(run)) {
            out.addAll(findMissingLoanPostings(organizationId, run));
        }
        return out;
    }

    private boolean isPayrollRunFinalized(PayrollRun run) {
        String s = run.getStatus();
        if (s == null) {
            return false;
        }
        String u = s.toUpperCase();
        return "APPROVED".equals(u) || "PROCESSED".equals(u) || "COMPLETED".equals(u) || "PAID".equals(u) || "CLOSED".equals(u);
    }

    private List<LoanRepaymentAnomalyDto> comparePayslipToLoanPostings(UUID organizationId, PayrollRun run) {
        List<LoanRepaymentAnomalyDto> out = new ArrayList<>();
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(run.getPayrollRunId());
        LocalDateTime detectedAt = LocalDateTime.now();
        for (PayrollDetail d : details) {
            if (!organizationId.equals(d.getOrganizationId())) {
                continue;
            }
            BigDecimal payslipLoan = sumLoanRepaymentOnPayslip(d.getPayrollDetailId());
            List<EmployeeLoan> activeLoans = employeeLoanRepository
                    .findByOrganizationIdAndEmployeeIdOrderByCreatedAtDesc(organizationId, d.getEmployeeId())
                    .stream()
                    .filter(l -> l.getStatus() == EmployeeLoanStatus.ACTIVE)
                    .toList();
            BigDecimal posted = BigDecimal.ZERO;
            UUID loanId = null;
            for (EmployeeLoan loan : activeLoans) {
                Optional<LoanRepaymentTransaction> tx = loanRepaymentTransactionRepository
                        .findByLoanIdAndPayrollRunIdAndSource(loan.getLoanId(), run.getPayrollRunId(), LoanRepaymentSource.PAYROLL);
                if (tx.isPresent()) {
                    posted = posted.add(tx.get().getAmount());
                    loanId = loan.getLoanId();
                }
            }
            BigDecimal payslipAbs = payslipLoan.abs().setScale(SCALE, ROUNDING);
            BigDecimal postedAbs = posted.abs().setScale(SCALE, ROUNDING);
            if (payslipAbs.compareTo(BigDecimal.ZERO) < 1 && postedAbs.compareTo(BigDecimal.ZERO) < 1) {
                continue;
            }
            if (payslipAbs.subtract(postedAbs).abs().compareTo(VARIANCE_TOLERANCE) > 0) {
                out.add(LoanRepaymentAnomalyDto.builder()
                        .type(LoanRepaymentAnomalyDto.AnomalyType.PAYSLIP_VS_LOAN_POSTING_MISMATCH)
                        .payrollRunId(run.getPayrollRunId())
                        .loanId(loanId)
                        .employeeId(d.getEmployeeId())
                        .payslipLoanAmount(payslipAbs)
                        .postedLoanAmount(postedAbs)
                        .varianceAmount(payslipLoan.subtract(posted).abs().setScale(SCALE, ROUNDING))
                        .message("Payslip loan repayment total differs from loan module PAYROLL posting for this run.")
                        .detectedAt(detectedAt)
                        .build());
            }
        }
        return out;
    }

    private BigDecimal sumLoanRepaymentOnPayslip(UUID payrollDetailId) {
        List<PayrollComponent> pcs = payrollComponentRepository.findByPayrollDetailId(payrollDetailId);
        BigDecimal sum = BigDecimal.ZERO;
        for (PayrollComponent pc : pcs) {
            Optional<SalaryComponent> scOpt = salaryComponentRepository.findById(pc.getComponentId());
            if (scOpt.isEmpty()) {
                continue;
            }
            SalaryComponent sc = scOpt.get();
            if (SalaryComponentCategory.LOAN_REPAYMENT.equals(sc.getCategory())) {
                sum = sum.add(pc.getAmount());
            }
        }
        return sum.setScale(SCALE, ROUNDING);
    }

    private List<LoanRepaymentAnomalyDto> findMissingLoanPostings(UUID organizationId, PayrollRun run) {
        List<LoanRecoveryLineDto> lines = getRecoveriesForPayroll(
                organizationId, run.getPayPeriodStart(), run.getPayPeriodEnd(), run.getPayrollRunId());
        List<LoanRepaymentAnomalyDto> out = new ArrayList<>();
        LocalDateTime detectedAt = LocalDateTime.now();
        for (LoanRecoveryLineDto line : lines) {
            if (line.getAmount() != null
                    && line.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && !line.isAlreadyPostedForRun()) {
                out.add(LoanRepaymentAnomalyDto.builder()
                        .type(LoanRepaymentAnomalyDto.AnomalyType.EXPECTED_RECOVERY_NOT_POSTED)
                        .payrollRunId(run.getPayrollRunId())
                        .loanId(line.getLoanId())
                        .employeeId(line.getEmployeeId())
                        .message("Loan recovery expected for this finalized payroll run but no PAYROLL posting exists.")
                        .detectedAt(detectedAt)
                        .build());
            }
        }
        return out;
    }

    @Transactional
    public void confirmPayrollDeductions(UUID payrollRunId, UUID organizationId, UUID actorUserId) {
        log.info("Loan payroll confirm: payrollRunId={} organizationId={} correlationId={}",
                payrollRunId, organizationId, IntegrationCorrelationIdHolder.get());
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run not found"));
        if (!organizationId.equals(run.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run does not belong to organization");
        }

        List<LoanRecoveryLineDto> lines = getRecoveriesForPayroll(
                organizationId, run.getPayPeriodStart(), run.getPayPeriodEnd(), payrollRunId);

        LocalDate paymentDate = run.getPaymentDate() != null ? run.getPaymentDate() : run.getPayPeriodEnd();

        for (LoanRecoveryLineDto line : lines) {
            if (line.isAlreadyPostedForRun() || line.getAmount() == null) {
                continue;
            }
            if (line.getAmount().compareTo(BigDecimal.ZERO) < 1) {
                continue;
            }
            try {
                payrollLoanRepaymentRecorder.recordPayrollRepayment(
                        organizationId,
                        line.getLoanId(),
                        line.getAmount(),
                        paymentDate,
                        payrollRunId,
                        actorUserId);
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    log.debug("Skipping loan {} payroll post - already recorded (concurrent confirm)", line.getLoanId());
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Sum of remaining principal on installments with due date on or before {@code periodEnd} (catch-up + current).
     */
    private BigDecimal computeRecoveryDueThrough(UUID loanId, LocalDate periodEnd) {
        List<LoanInstallment> installments = loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId);
        BigDecimal sum = BigDecimal.ZERO;
        for (LoanInstallment i : installments) {
            BigDecimal rem = i.getScheduledAmount().subtract(i.getPaidAmount());
            if (rem.compareTo(BigDecimal.ZERO) < 1) {
                continue;
            }
            if (!i.getDueDate().isAfter(periodEnd)) {
                sum = sum.add(rem);
            }
        }
        return sum.setScale(SCALE, ROUNDING);
    }
}
