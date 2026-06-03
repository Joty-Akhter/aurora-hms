package com.easyops.hr.service;

import com.easyops.hr.dto.LoanBulkHolidayRecalcResultDto;
import com.easyops.hr.dto.LoanOrganizationSettingsDto;
import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.Holiday;
import com.easyops.hr.entity.LoanAuditLog;
import com.easyops.hr.entity.LoanHolidayShiftMode;
import com.easyops.hr.entity.LoanInstallment;
import com.easyops.hr.entity.LoanInstallmentStatus;
import com.easyops.hr.integration.PfSettlementClient;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.repository.LoanNotificationEventRepository;
import com.easyops.hr.repository.HolidayRepository;
import com.easyops.hr.repository.LoanAuditLogRepository;
import com.easyops.hr.repository.LoanInstallmentRepository;
import com.easyops.hr.repository.LoanRepaymentAllocationRepository;
import com.easyops.hr.repository.LoanRepaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeLoanServiceHolidayTest {

    @Mock
    private EmployeeLoanRepository employeeLoanRepository;
    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;
    @Mock
    private LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;
    @Mock
    private LoanRepaymentAllocationRepository loanRepaymentAllocationRepository;
    @Mock
    private LoanOrgSettingsProvider loanOrgSettingsProvider;
    @Mock
    private LoanAuditLogRepository loanAuditLogRepository;
    @Mock
    private PfSettlementClient pfSettlementClient;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private LoanCategoryRepository loanCategoryRepository;
    @Mock
    private LoanNotificationEventRepository loanNotificationEventRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private TransactionTemplate transactionTemplate;
    private LoanNotificationService loanNotificationService;
    private EmployeeLoanService employeeLoanService;

    private UUID orgId;
    private UUID loanId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        loanId = UUID.randomUUID();
        PlatformTransactionManager noopTm = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                // no-op for unit tests
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                // no-op for unit tests
            }
        };
        transactionTemplate = new TransactionTemplate(noopTm);
        LoanAuditService loanAuditService = new LoanAuditService(loanAuditLogRepository);
        loanNotificationService = new LoanNotificationService(loanNotificationEventRepository, employeeRepository);
        employeeLoanService = new EmployeeLoanService(
                employeeLoanRepository,
                loanInstallmentRepository,
                loanRepaymentTransactionRepository,
                loanRepaymentAllocationRepository,
                loanOrgSettingsProvider,
                loanAuditService,
                pfSettlementClient,
                holidayRepository,
                transactionTemplate,
                loanCategoryRepository,
                loanNotificationService);
    }

    @Test
    void buildEqualPrincipalSchedule_shiftOff_usesRawCalendarDates() throws Exception {
        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings(false, LoanHolidayShiftMode.NEXT_BUSINESS_DAY));
        when(holidayRepository.findOrgWideActiveHolidaysInRange(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<LoanInstallment> rows = invokeBuildSchedule(LocalDate.of(2024, 2, 2), 3);
        assertEquals(LocalDate.of(2024, 3, 2), rows.get(0).getDueDate());
        assertEquals(LocalDate.of(2024, 4, 2), rows.get(1).getDueDate());
        assertEquals(LocalDate.of(2024, 5, 2), rows.get(2).getDueDate());
    }

    @Test
    void buildEqualPrincipalSchedule_shiftOn_movesSaturdayToMonday() throws Exception {
        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings(true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY));
        when(holidayRepository.findOrgWideActiveHolidaysInRange(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<LoanInstallment> rows = invokeBuildSchedule(LocalDate.of(2024, 2, 2), 3);
        assertEquals(LocalDate.of(2024, 3, 4), rows.get(0).getDueDate());
        assertEquals(LocalDate.of(2024, 4, 2), rows.get(1).getDueDate());
        assertEquals(LocalDate.of(2024, 5, 2), rows.get(2).getDueDate());
    }

    @Test
    void recalculate_skipsFullyPaidAndSkippedStatus() {
        EmployeeLoan loan = baseLoan();
        when(employeeLoanRepository.findByLoanIdAndOrganizationId(loanId, orgId)).thenReturn(Optional.of(loan));
        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings(true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY));
        when(holidayRepository.findOrgWideActiveHolidaysInRange(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        LoanInstallment i1 = installment(1, LocalDate.of(2024, 3, 1), BigDecimal.ZERO);
        LoanInstallment i2 = installment(2, LocalDate.of(2024, 4, 1), new BigDecimal("1.00"));
        LoanInstallment i3 = installment(3, LocalDate.of(2024, 5, 1), new BigDecimal("10.00"));
        i3.setScheduledAmount(new BigDecimal("10.00"));
        LoanInstallment i4 = installment(4, LocalDate.of(2024, 6, 1), BigDecimal.ZERO);
        i4.setStatus(LoanInstallmentStatus.SKIPPED);
        i4.setSkipReason("approved");

        List<LoanInstallment> list = new ArrayList<>(List.of(i1, i2, i3, i4));
        when(loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId)).thenReturn(list);

        employeeLoanService.recalculateInstallmentDueDatesForHolidays(orgId, loanId, UUID.randomUUID());

        ArgumentCaptor<List<LoanInstallment>> cap = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(cap.capture());
        List<LoanInstallment> saved = cap.getValue();
        assertEquals(LocalDate.of(2024, 3, 4), saved.get(0).getDueDate());
        assertEquals(LocalDate.of(2024, 4, 2), saved.get(1).getDueDate());
        assertEquals(LocalDate.of(2024, 5, 1), saved.get(2).getDueDate());
        assertEquals(LocalDate.of(2024, 6, 1), saved.get(3).getDueDate());

        ArgumentCaptor<LoanAuditLog> auditCap = ArgumentCaptor.forClass(LoanAuditLog.class);
        verify(loanAuditLogRepository).save(auditCap.capture());
        assertEquals(LoanAuditLog.ACTION_INSTALLMENT_DUE_DATES_RECALC, auditCap.getValue().getAction());
    }

    @Test
    void recalculateInstallmentDueDatesForHolidays_shiftsOrgHoliday() {
        EmployeeLoan loan = baseLoan();
        loan.setDisbursementDate(LocalDate.of(2024, 1, 2));
        when(employeeLoanRepository.findByLoanIdAndOrganizationId(loanId, orgId)).thenReturn(Optional.of(loan));
        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings(true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY));

        Holiday h = new Holiday();
        h.setHolidayDate(LocalDate.of(2024, 2, 2));
        h.setIsActive(true);
        when(holidayRepository.findOrgWideActiveHolidaysInRange(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(h));

        LoanInstallment i1 = installment(1, LocalDate.of(2024, 2, 1), BigDecimal.ZERO);
        List<LoanInstallment> list = new ArrayList<>(List.of(i1));
        when(loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(loanId)).thenReturn(list);

        employeeLoanService.recalculateInstallmentDueDatesForHolidays(orgId, loanId, UUID.randomUUID());

        ArgumentCaptor<List<LoanInstallment>> cap = ArgumentCaptor.forClass(List.class);
        verify(loanInstallmentRepository).saveAll(cap.capture());
        assertEquals(LocalDate.of(2024, 2, 5), cap.getValue().get(0).getDueDate());
    }

    /**
     * AD-03 bulk: multiple loans, skip without disbursement, mixed success/failure, one org audit row,
     * and per-loan {@link TransactionTemplate} boundaries (commit vs rollback).
     */
    @Test
    void recalculateInstallmentDueDatesForHolidaysAll_mixedSuccessFailure_skipsCountsFailuresTransactionsAndAudit() {
        CountingTransactionManager ctm = new CountingTransactionManager();
        TransactionTemplate tt = new TransactionTemplate(ctm);
        LoanAuditService las = new LoanAuditService(loanAuditLogRepository);
        EmployeeLoanService svc = new EmployeeLoanService(
                employeeLoanRepository,
                loanInstallmentRepository,
                loanRepaymentTransactionRepository,
                loanRepaymentAllocationRepository,
                loanOrgSettingsProvider,
                las,
                pfSettlementClient,
                holidayRepository,
                tt,
                loanCategoryRepository,
                loanNotificationService);

        UUID loanSkip = UUID.randomUUID();
        UUID loanOk1 = UUID.randomUUID();
        UUID loanFail = UUID.randomUUID();
        UUID loanOk2 = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        EmployeeLoan skipped = loanWithoutDisbursement(loanSkip);
        EmployeeLoan ok1 = activeLoanWithId(loanOk1);
        EmployeeLoan failLoan = activeLoanWithId(loanFail);
        EmployeeLoan ok2 = activeLoanWithId(loanOk2);

        when(employeeLoanRepository.findByOrganizationIdAndStatusIn(eq(orgId), any()))
                .thenReturn(List.of(skipped, ok1, failLoan, ok2));

        lenient().when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings(true, LoanHolidayShiftMode.NEXT_BUSINESS_DAY));
        lenient().when(holidayRepository.findOrgWideActiveHolidaysInRange(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(employeeLoanRepository.findByLoanIdAndOrganizationId(any(UUID.class), eq(orgId)))
                .thenAnswer(inv -> {
                    UUID lid = inv.getArgument(0);
                    if (lid.equals(loanFail)) {
                        return Optional.empty();
                    }
                    if (lid.equals(loanOk1)) {
                        return Optional.of(ok1);
                    }
                    if (lid.equals(loanOk2)) {
                        return Optional.of(ok2);
                    }
                    return Optional.empty();
                });

        when(loanInstallmentRepository.findByLoanIdOrderBySequenceNumberAsc(any(UUID.class)))
                .thenAnswer(inv -> {
                    UUID lid = inv.getArgument(0);
                    if (lid.equals(loanOk1) || lid.equals(loanOk2)) {
                        return new ArrayList<>(List.of(installment(1, LocalDate.of(2024, 3, 1), BigDecimal.ZERO)));
                    }
                    return Collections.emptyList();
                });

        LoanBulkHolidayRecalcResultDto result = svc.recalculateInstallmentDueDatesForHolidaysAll(orgId, actor);

        assertEquals(orgId, result.getOrganizationId());
        assertEquals(2, result.getLoansRecalculated());
        assertEquals(1, result.getLoansSkipped());
        assertEquals(1, result.getFailures().size());
        assertEquals(loanFail, result.getFailures().get(0).getLoanId());
        assertTrue(
                result.getFailures().get(0).getMessage() != null
                        && result.getFailures().get(0).getMessage().toLowerCase().contains("loan"));

        assertEquals(2, ctm.commits, "each successful loan commits its own transaction");
        assertEquals(1, ctm.rollbacks, "failed loan rolls back its transaction");

        verify(loanInstallmentRepository, times(2)).saveAll(any());

        ArgumentCaptor<LoanAuditLog> auditCap = ArgumentCaptor.forClass(LoanAuditLog.class);
        verify(loanAuditLogRepository).save(auditCap.capture());
        assertEquals(LoanAuditLog.ENTITY_LOAN_ORG, auditCap.getValue().getEntityType());
        assertEquals(LoanAuditLog.ACTION_BULK_HOLIDAY_RECALC_COMPLETED, auditCap.getValue().getAction());
        assertTrue(auditCap.getValue().getNewValues().contains("failed=1"));
    }

    /** Tracks Spring {@link TransactionTemplate} commit vs rollback for per-loan isolation. */
    private static final class CountingTransactionManager implements PlatformTransactionManager {
        int commits;
        int rollbacks;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commits++;
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbacks++;
        }
    }

    private EmployeeLoan loanWithoutDisbursement(UUID id) {
        EmployeeLoan loan = baseLoan();
        loan.setLoanId(id);
        loan.setDisbursementDate(null);
        return loan;
    }

    private EmployeeLoan activeLoanWithId(UUID id) {
        EmployeeLoan loan = baseLoan();
        loan.setLoanId(id);
        return loan;
    }

    private List<LoanInstallment> invokeBuildSchedule(LocalDate disbursementDate, int tenure) throws Exception {
        Method m = EmployeeLoanService.class.getDeclaredMethod(
                "buildEqualPrincipalSchedule",
                UUID.class,
                UUID.class,
                BigDecimal.class,
                int.class,
                LocalDate.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<LoanInstallment> rows = (List<LoanInstallment>) m.invoke(
                employeeLoanService,
                orgId,
                loanId,
                new BigDecimal("300.00"),
                tenure,
                disbursementDate);
        return rows;
    }

    private static LoanOrganizationSettingsDto settings(boolean shift, LoanHolidayShiftMode mode) {
        return LoanOrganizationSettingsDto.builder()
                .shiftInstallmentDueDatesForHolidays(shift)
                .loanHolidayShiftMode(mode)
                .build();
    }

    private EmployeeLoan baseLoan() {
        EmployeeLoan loan = new EmployeeLoan();
        loan.setLoanId(loanId);
        loan.setOrganizationId(orgId);
        loan.setEmployeeId(UUID.randomUUID());
        loan.setCategoryId(UUID.randomUUID());
        loan.setPrincipalAmount(new BigDecimal("100.00"));
        loan.setOutstandingBalance(new BigDecimal("100.00"));
        loan.setCurrency("BDT");
        loan.setTenureMonths(12);
        loan.setDisbursementDate(LocalDate.of(2024, 2, 2));
        loan.setStatus(EmployeeLoanStatus.ACTIVE);
        return loan;
    }

    private static LoanInstallment installment(int seq, LocalDate due, BigDecimal paid) {
        LoanInstallment i = new LoanInstallment();
        i.setSequenceNumber(seq);
        i.setDueDate(due);
        i.setScheduledAmount(new BigDecimal("10.00"));
        i.setPaidAmount(paid);
        i.setStatus(LoanInstallmentStatus.DUE);
        return i;
    }
}
