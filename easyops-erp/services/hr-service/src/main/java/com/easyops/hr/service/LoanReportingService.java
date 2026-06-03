package com.easyops.hr.service;

import com.easyops.hr.dto.LoanAccountingDisbursementLineDto;
import com.easyops.hr.dto.LoanAccountingExportDto;
import com.easyops.hr.dto.LoanAccountingRepaymentLineDto;
import com.easyops.hr.dto.LoanArrearsRowDto;
import com.easyops.hr.dto.LoanRegisterRowDto;
import com.easyops.hr.constants.LoanAccountingCoaKeys;
import com.easyops.hr.dto.LoanReportSummaryDto;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.LoanAccountingCoaMapping;
import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanCategory;
import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.entity.LoanInstallment;
import com.easyops.hr.entity.LoanRepaymentTransaction;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.repository.LoanAccountingCoaMappingRepository;
import com.easyops.hr.repository.LoanInstallmentRepository;
import com.easyops.hr.repository.LoanRepaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RE-01: operational loan reports (active, arrears, register, settlement at exit).
 */
@Service
@RequiredArgsConstructor
public class LoanReportingService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2;

    /** PI-05 journal suggestions (map to COA codes in accounting). */
    private static final String SUGGESTED_DR_DISBURSE = "Employee loan receivable";
    private static final String SUGGESTED_CR_DISBURSE = "Cash / bank";
    private static final String SUGGESTED_DR_REPAY = "Cash / bank";
    private static final String SUGGESTED_CR_REPAY = "Employee loan receivable";

    private final EmployeeLoanRepository employeeLoanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanCategoryRepository loanCategoryRepository;
    private final LoanOrgSettingsProvider loanOrgSettingsProvider;
    private final LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;
    private final LoanAccountingCoaMappingRepository loanAccountingCoaMappingRepository;

    @Transactional(readOnly = true)
    public LoanReportSummaryDto getSummary(UUID organizationId) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<EmployeeLoan> all = employeeLoanRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        long active = 0;
        long pending = 0;
        long settlement = 0;
        long closed = 0;
        BigDecimal totalOs = BigDecimal.ZERO;
        for (EmployeeLoan l : all) {
            if (l.getStatus() == EmployeeLoanStatus.ACTIVE) {
                active++;
            } else if (l.getStatus() == EmployeeLoanStatus.PENDING_DISBURSEMENT) {
                pending++;
            } else if (l.getStatus() == EmployeeLoanStatus.SETTLEMENT_PENDING) {
                settlement++;
            } else if (l.getStatus() == EmployeeLoanStatus.CLOSED) {
                closed++;
            }
            if (l.getOutstandingBalance() != null) {
                totalOs = totalOs.add(l.getOutstandingBalance());
            }
        }
        List<LoanInstallment> arrears = loanInstallmentRepository.findArrearInstallments(
                organizationId,
                LocalDate.now(),
                EnumSet.of(EmployeeLoanStatus.ACTIVE, EmployeeLoanStatus.SETTLEMENT_PENDING));
        BigDecimal arrearsSum = BigDecimal.ZERO;
        for (LoanInstallment i : arrears) {
            BigDecimal rem = i.getScheduledAmount().subtract(i.getPaidAmount());
            if (rem.compareTo(BigDecimal.ZERO) > 0) {
                arrearsSum = arrearsSum.add(rem);
            }
        }
        return LoanReportSummaryDto.builder()
                .activeLoanCount(active)
                .pendingDisbursementCount(pending)
                .settlementPendingCount(settlement)
                .closedLoanCount(closed)
                .totalOutstanding(totalOs.setScale(SCALE, ROUNDING))
                .totalArrearsRemaining(arrearsSum.setScale(SCALE, ROUNDING))
                .arrearsInstallmentCount(arrears.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LoanRegisterRowDto> getRegister(
            UUID organizationId,
            UUID categoryId,
            UUID employeeId,
            EmployeeLoanStatus status,
            LoanCategoryType categoryType) {
        loanOrgSettingsProvider.getSettings(organizationId);
        List<EmployeeLoan> loans = employeeLoanRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        if (employeeId != null) {
            loans = loans.stream().filter(l -> employeeId.equals(l.getEmployeeId())).toList();
        }
        if (categoryId != null) {
            loans = loans.stream().filter(l -> categoryId.equals(l.getCategoryId())).toList();
        }
        if (status != null) {
            loans = loans.stream().filter(l -> status.equals(l.getStatus())).toList();
        }
        Map<UUID, Employee> empMap = loadEmployees(organizationId, loans);
        Map<UUID, LoanCategory> catMap = loadCategories(organizationId, loans);
        List<LoanRegisterRowDto> rows = new ArrayList<>();
        for (EmployeeLoan l : loans) {
            Employee e = empMap.get(l.getEmployeeId());
            LoanCategory c = catMap.get(l.getCategoryId());
            if (categoryType != null && (c == null || c.getCategoryType() != categoryType)) {
                continue;
            }
            rows.add(LoanRegisterRowDto.builder()
                    .loanId(l.getLoanId())
                    .employeeId(l.getEmployeeId())
                    .employeeName(e != null ? e.getName() : null)
                    .employeeNumber(e != null ? e.getEmployeeNumber() : null)
                    .categoryId(l.getCategoryId())
                    .categoryName(c != null ? c.getName() : null)
                    .categoryType(c != null ? c.getCategoryType() : null)
                    .principalAmount(l.getPrincipalAmount())
                    .outstandingBalance(l.getOutstandingBalance())
                    .currency(l.getCurrency())
                    .status(l.getStatus())
                    .disbursementDate(l.getDisbursementDate())
                    .settlementShortfallAmount(l.getSettlementShortfallAmount())
                    .separationEffectiveDate(l.getSeparationEffectiveDate())
                    .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<LoanArrearsRowDto> getArrears(UUID organizationId, LocalDate asOf) {
        LocalDate ref = asOf != null ? asOf : LocalDate.now();
        loanOrgSettingsProvider.getSettings(organizationId);
        List<LoanInstallment> insts = loanInstallmentRepository.findArrearInstallments(
                organizationId,
                ref,
                EnumSet.of(EmployeeLoanStatus.ACTIVE, EmployeeLoanStatus.SETTLEMENT_PENDING));
        if (insts.isEmpty()) {
            return List.of();
        }
        Set<UUID> loanIds = insts.stream().map(LoanInstallment::getLoanId).collect(Collectors.toSet());
        Map<UUID, EmployeeLoan> loanMap = employeeLoanRepository.findAllById(loanIds).stream()
                .filter(l -> organizationId.equals(l.getOrganizationId()))
                .collect(Collectors.toMap(EmployeeLoan::getLoanId, l -> l, (a, b) -> a));
        Map<UUID, Employee> empMap = loadEmployees(organizationId, loanMap.values());
        Map<UUID, LoanCategory> catMap = loadCategories(organizationId, loanMap.values());
        List<LoanArrearsRowDto> rows = new ArrayList<>();
        for (LoanInstallment i : insts) {
            EmployeeLoan loan = loanMap.get(i.getLoanId());
            if (loan == null) {
                continue;
            }
            Employee e = empMap.get(loan.getEmployeeId());
            LoanCategory c = catMap.get(loan.getCategoryId());
            BigDecimal rem = i.getScheduledAmount().subtract(i.getPaidAmount()).max(BigDecimal.ZERO);
            long days = ChronoUnit.DAYS.between(i.getDueDate(), ref);
            rows.add(LoanArrearsRowDto.builder()
                    .loanId(loan.getLoanId())
                    .employeeId(loan.getEmployeeId())
                    .employeeName(e != null ? e.getName() : null)
                    .employeeNumber(e != null ? e.getEmployeeNumber() : null)
                    .categoryName(c != null ? c.getName() : null)
                    .installmentId(i.getInstallmentId())
                    .sequenceNumber(i.getSequenceNumber())
                    .dueDate(i.getDueDate())
                    .scheduledAmount(i.getScheduledAmount())
                    .paidAmount(i.getPaidAmount())
                    .remainingAmount(rem.setScale(SCALE, ROUNDING))
                    .daysPastDue(days)
                    .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<LoanRegisterRowDto> getSettlementExitRegister(UUID organizationId) {
        return getRegister(organizationId, null, null, EmployeeLoanStatus.SETTLEMENT_PENDING, null);
    }

    /**
     * Phase 7 (PI-05): disbursements and repayments in a period for finance reconciliation / journal staging.
     */
    @Transactional(readOnly = true)
    public LoanAccountingExportDto getAccountingExport(UUID organizationId, LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom == null || periodTo == null || periodFrom.isAfter(periodTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "periodFrom and periodTo are required; from must be <= to");
        }
        var settings = loanOrgSettingsProvider.getSettings(organizationId);
        String currency = settings.getCurrency();

        List<EmployeeLoan> disbursedLoans = employeeLoanRepository.findDisbursementsInPeriod(organizationId, periodFrom, periodTo);
        List<LoanRepaymentTransaction> repayments =
                loanRepaymentTransactionRepository.findByOrganizationAndPaymentDateBetween(organizationId, periodFrom, periodTo);

        Set<UUID> repayLoanIds = repayments.stream().map(LoanRepaymentTransaction::getLoanId).collect(Collectors.toSet());
        Map<UUID, EmployeeLoan> repayLoanMap = employeeLoanRepository.findAllById(repayLoanIds).stream()
                .filter(l -> organizationId.equals(l.getOrganizationId()))
                .collect(Collectors.toMap(EmployeeLoan::getLoanId, l -> l, (a, b) -> a));

        final Map<UUID, Employee> empMap = new HashMap<>(loadEmployees(organizationId, disbursedLoans));
        empMap.putAll(loadEmployees(organizationId, repayLoanMap.values()));
        final Map<UUID, LoanCategory> catMap = new HashMap<>(loadCategories(organizationId, disbursedLoans));
        for (EmployeeLoan l : repayLoanMap.values()) {
            loanCategoryRepository.findByCategoryIdAndOrganizationId(l.getCategoryId(), organizationId)
                    .ifPresent(c -> catMap.put(c.getCategoryId(), c));
        }

        Optional<LoanAccountingCoaMapping> coaDisb =
                loanAccountingCoaMappingRepository.findByOrganizationIdAndMappingKey(
                        organizationId, LoanAccountingCoaKeys.LOAN_DISBURSEMENT);
        Optional<LoanAccountingCoaMapping> coaRep =
                loanAccountingCoaMappingRepository.findByOrganizationIdAndMappingKey(
                        organizationId, LoanAccountingCoaKeys.LOAN_REPAYMENT);

        BigDecimal totalDisb = BigDecimal.ZERO;
        List<LoanAccountingDisbursementLineDto> disbLines = new ArrayList<>();
        for (EmployeeLoan l : disbursedLoans) {
            BigDecimal amt = l.getDisbursedAmount() != null ? l.getDisbursedAmount() : l.getPrincipalAmount();
            if (amt == null) {
                continue;
            }
            amt = amt.setScale(SCALE, ROUNDING);
            totalDisb = totalDisb.add(amt);
            Employee e = empMap.get(l.getEmployeeId());
            LoanCategory c = catMap.get(l.getCategoryId());
            String memo = "Loan disbursement loanId=" + l.getLoanId() + (c != null ? " category=" + c.getName() : "");
            disbLines.add(LoanAccountingDisbursementLineDto.builder()
                    .loanId(l.getLoanId())
                    .employeeId(l.getEmployeeId())
                    .employeeNumber(e != null ? e.getEmployeeNumber() : null)
                    .employeeName(e != null ? e.getName() : null)
                    .categoryId(l.getCategoryId())
                    .categoryName(c != null ? c.getName() : null)
                    .disbursementDate(l.getDisbursementDate())
                    .amount(amt)
                    .currency(l.getCurrency() != null ? l.getCurrency() : currency)
                    .journalMemo(memo)
                    .suggestedDebitAccount(SUGGESTED_DR_DISBURSE)
                    .suggestedCreditAccount(SUGGESTED_CR_DISBURSE)
                    .coaDebitCode(coaDisb.map(LoanAccountingCoaMapping::getDebitAccountCode).orElse(null))
                    .coaCreditCode(coaDisb.map(LoanAccountingCoaMapping::getCreditAccountCode).orElse(null))
                    .build());
        }

        BigDecimal totalRep = BigDecimal.ZERO;
        List<LoanAccountingRepaymentLineDto> repLines = new ArrayList<>();
        for (LoanRepaymentTransaction t : repayments) {
            EmployeeLoan loan = repayLoanMap.get(t.getLoanId());
            if (loan == null) {
                continue;
            }
            BigDecimal amt = t.getAmount().setScale(SCALE, ROUNDING);
            totalRep = totalRep.add(amt);
            Employee e = empMap.get(loan.getEmployeeId());
            LoanCategory c = catMap.get(loan.getCategoryId());
            String memo = "Loan repayment " + t.getSource() + " loanId=" + loan.getLoanId();
            repLines.add(LoanAccountingRepaymentLineDto.builder()
                    .transactionId(t.getTransactionId())
                    .loanId(loan.getLoanId())
                    .employeeId(loan.getEmployeeId())
                    .employeeNumber(e != null ? e.getEmployeeNumber() : null)
                    .employeeName(e != null ? e.getName() : null)
                    .categoryId(loan.getCategoryId())
                    .categoryName(c != null ? c.getName() : null)
                    .paymentDate(t.getPaymentDate())
                    .amount(amt)
                    .currency(loan.getCurrency() != null ? loan.getCurrency() : currency)
                    .source(t.getSource())
                    .payrollRunId(t.getPayrollRunId())
                    .notes(t.getNotes())
                    .journalMemo(memo)
                    .suggestedDebitAccount(SUGGESTED_DR_REPAY)
                    .suggestedCreditAccount(SUGGESTED_CR_REPAY)
                    .coaDebitCode(coaRep.map(LoanAccountingCoaMapping::getDebitAccountCode).orElse(null))
                    .coaCreditCode(coaRep.map(LoanAccountingCoaMapping::getCreditAccountCode).orElse(null))
                    .build());
        }

        return LoanAccountingExportDto.builder()
                .organizationId(organizationId)
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .currency(currency)
                .disbursements(disbLines)
                .repayments(repLines)
                .totalDisbursements(totalDisb.setScale(SCALE, ROUNDING))
                .totalRepayments(totalRep.setScale(SCALE, ROUNDING))
                .build();
    }

    public String accountingDisbursementsToCsv(LoanAccountingExportDto export) {
        StringBuilder sb = new StringBuilder();
        sb.append("loanId,employeeId,employeeNumber,employeeName,categoryName,disbursementDate,amount,currency,"
                + "journalMemo,suggestedDebitAccount,suggestedCreditAccount,coaDebitCode,coaCreditCode\n");
        for (LoanAccountingDisbursementLineDto r : export.getDisbursements()) {
            sb.append(csvField(r.getLoanId()))
                    .append(',')
                    .append(csvField(r.getEmployeeId()))
                    .append(',')
                    .append(csvField(r.getEmployeeNumber()))
                    .append(',')
                    .append(csvField(r.getEmployeeName()))
                    .append(',')
                    .append(csvField(r.getCategoryName()))
                    .append(',')
                    .append(csvField(r.getDisbursementDate()))
                    .append(',')
                    .append(csvField(r.getAmount()))
                    .append(',')
                    .append(csvField(r.getCurrency()))
                    .append(',')
                    .append(csvField(r.getJournalMemo()))
                    .append(',')
                    .append(csvField(r.getSuggestedDebitAccount()))
                    .append(',')
                    .append(csvField(r.getSuggestedCreditAccount()))
                    .append(',')
                    .append(csvField(r.getCoaDebitCode()))
                    .append(',')
                    .append(csvField(r.getCoaCreditCode()))
                    .append('\n');
        }
        return sb.toString();
    }

    public String accountingRepaymentsToCsv(LoanAccountingExportDto export) {
        StringBuilder sb = new StringBuilder();
        sb.append("transactionId,loanId,employeeId,employeeNumber,employeeName,categoryName,paymentDate,amount,currency,"
                + "source,payrollRunId,notes,journalMemo,suggestedDebitAccount,suggestedCreditAccount,coaDebitCode,coaCreditCode\n");
        for (LoanAccountingRepaymentLineDto r : export.getRepayments()) {
            sb.append(csvField(r.getTransactionId()))
                    .append(',')
                    .append(csvField(r.getLoanId()))
                    .append(',')
                    .append(csvField(r.getEmployeeId()))
                    .append(',')
                    .append(csvField(r.getEmployeeNumber()))
                    .append(',')
                    .append(csvField(r.getEmployeeName()))
                    .append(',')
                    .append(csvField(r.getCategoryName()))
                    .append(',')
                    .append(csvField(r.getPaymentDate()))
                    .append(',')
                    .append(csvField(r.getAmount()))
                    .append(',')
                    .append(csvField(r.getCurrency()))
                    .append(',')
                    .append(csvField(r.getSource()))
                    .append(',')
                    .append(csvField(r.getPayrollRunId()))
                    .append(',')
                    .append(csvField(r.getNotes()))
                    .append(',')
                    .append(csvField(r.getJournalMemo()))
                    .append(',')
                    .append(csvField(r.getSuggestedDebitAccount()))
                    .append(',')
                    .append(csvField(r.getSuggestedCreditAccount()))
                    .append(',')
                    .append(csvField(r.getCoaDebitCode()))
                    .append(',')
                    .append(csvField(r.getCoaCreditCode()))
                    .append('\n');
        }
        return sb.toString();
    }

    /** CSV export (RE-01); call after {@link #getRegister} / {@link #getArrears} in controller to keep transactions valid. */
    public String registerRowsToCsv(List<LoanRegisterRowDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("loanId,employeeId,employeeName,employeeNumber,categoryName,categoryType,principal,outstanding,currency,status,disbursementDate,settlementShortfall,separationDate\n");
        for (LoanRegisterRowDto r : rows) {
            sb.append(csvField(r.getLoanId()))
                    .append(',')
                    .append(csvField(r.getEmployeeId()))
                    .append(',')
                    .append(csvField(r.getEmployeeName()))
                    .append(',')
                    .append(csvField(r.getEmployeeNumber()))
                    .append(',')
                    .append(csvField(r.getCategoryName()))
                    .append(',')
                    .append(csvField(r.getCategoryType()))
                    .append(',')
                    .append(csvField(r.getPrincipalAmount()))
                    .append(',')
                    .append(csvField(r.getOutstandingBalance()))
                    .append(',')
                    .append(csvField(r.getCurrency()))
                    .append(',')
                    .append(csvField(r.getStatus()))
                    .append(',')
                    .append(csvField(r.getDisbursementDate()))
                    .append(',')
                    .append(csvField(r.getSettlementShortfallAmount()))
                    .append(',')
                    .append(csvField(r.getSeparationEffectiveDate()))
                    .append('\n');
        }
        return sb.toString();
    }

    public String arrearsRowsToCsv(List<LoanArrearsRowDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("loanId,employeeId,employeeName,categoryName,sequence,dueDate,scheduled,paid,remaining,daysPastDue\n");
        for (LoanArrearsRowDto r : rows) {
            sb.append(csvField(r.getLoanId()))
                    .append(',')
                    .append(csvField(r.getEmployeeId()))
                    .append(',')
                    .append(csvField(r.getEmployeeName()))
                    .append(',')
                    .append(csvField(r.getCategoryName()))
                    .append(',')
                    .append(csvField(r.getSequenceNumber()))
                    .append(',')
                    .append(csvField(r.getDueDate()))
                    .append(',')
                    .append(csvField(r.getScheduledAmount()))
                    .append(',')
                    .append(csvField(r.getPaidAmount()))
                    .append(',')
                    .append(csvField(r.getRemainingAmount()))
                    .append(',')
                    .append(csvField(r.getDaysPastDue()))
                    .append('\n');
        }
        return sb.toString();
    }

    private Map<UUID, Employee> loadEmployees(UUID organizationId, Collection<EmployeeLoan> loans) {
        Set<UUID> ids = loans.stream().map(EmployeeLoan::getEmployeeId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(ids).stream()
                .filter(e -> organizationId.equals(e.getOrganizationId()))
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));
    }

    private Map<UUID, LoanCategory> loadCategories(UUID organizationId, Collection<EmployeeLoan> loans) {
        Set<UUID> ids = loans.stream().map(EmployeeLoan::getCategoryId).collect(Collectors.toSet());
        Map<UUID, LoanCategory> m = new HashMap<>();
        for (UUID cid : ids) {
            loanCategoryRepository.findByCategoryIdAndOrganizationId(cid, organizationId).ifPresent(c -> m.put(cid, c));
        }
        return m;
    }

    private static String csvField(Object o) {
        if (o == null) {
            return "";
        }
        String s = o.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
