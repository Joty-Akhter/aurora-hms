package com.easyops.hr.service;

import com.easyops.hr.dto.LoanOrganizationSettingsDto;
import com.easyops.hr.entity.*;
import com.easyops.hr.repository.EmployeeLoanRepository;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.LoanApplicationActionRepository;
import com.easyops.hr.repository.LoanApplicationRepository;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.repository.LoanNotificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanApplicationActionRepository loanApplicationActionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LoanCategoryRepository loanCategoryRepository;
    @Mock
    private LoanOrgSettingsProvider loanOrgSettingsProvider;
    @Mock
    private ApprovedLoanCreator approvedLoanCreator;
    @Mock
    private EmployeeLoanRepository employeeLoanRepository;
    @Mock
    private LoanNotificationEventRepository loanNotificationEventRepository;

    private LoanApplicationService loanApplicationService;

    private UUID orgId;
    private UUID employeeId;
    private UUID categoryId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        LoanNotificationService notifications = new LoanNotificationService(loanNotificationEventRepository, employeeRepository);
        loanApplicationService = new LoanApplicationService(
                loanApplicationRepository,
                loanApplicationActionRepository,
                employeeRepository,
                loanCategoryRepository,
                loanOrgSettingsProvider,
                approvedLoanCreator,
                employeeLoanRepository,
                notifications);
    }

    @Test
    void submit_throwsWhenTenureNotMet() {
        LoanApplication app = draftApplication();
        when(loanApplicationRepository.findByApplicationIdAndOrganizationId(applicationId, orgId)).thenReturn(Optional.of(app));

        LoanOrganizationSettingsDto settings = settingsDto();
        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settings);

        LoanCategory category = activeCategory(LoanCategoryType.TERM_LOAN);
        when(loanCategoryRepository.findByCategoryIdAndOrganizationId(categoryId, orgId)).thenReturn(Optional.of(category));

        Employee emp = new Employee();
        emp.setEmployeeId(employeeId);
        emp.setOrganizationId(orgId);
        emp.setHireDate(LocalDate.now().minusMonths(3));
        emp.setIsActive(true);
        emp.setEmploymentStatus("ACTIVE");
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));

        assertThrows(ResponseStatusException.class,
                () -> loanApplicationService.submit(orgId, applicationId, UUID.randomUUID()));
    }

    @Test
    void submit_throwsWhenConflictingPendingApplication() {
        LoanApplication app = draftApplication();
        when(loanApplicationRepository.findByApplicationIdAndOrganizationId(applicationId, orgId)).thenReturn(Optional.of(app));

        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settingsDto());

        LoanCategory category = activeCategory(LoanCategoryType.TERM_LOAN);
        when(loanCategoryRepository.findByCategoryIdAndOrganizationId(categoryId, orgId)).thenReturn(Optional.of(category));

        Employee emp = eligibleEmployee();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));

        LoanApplication other = new LoanApplication();
        other.setApplicationId(UUID.randomUUID());
        when(loanApplicationRepository.findBlockingApplications(eq(orgId), eq(employeeId), anyList(), eq(applicationId)))
                .thenReturn(List.of(other));

        assertThrows(ResponseStatusException.class,
                () -> loanApplicationService.submit(orgId, applicationId, UUID.randomUUID()));
    }

    @Test
    void submit_succeedsWhenEligible() {
        LoanApplication app = draftApplication();
        when(loanApplicationRepository.findByApplicationIdAndOrganizationId(applicationId, orgId)).thenReturn(Optional.of(app));

        when(loanOrgSettingsProvider.getSettings(orgId)).thenReturn(settingsDto());

        LoanCategory category = activeCategory(LoanCategoryType.TERM_LOAN);
        when(loanCategoryRepository.findByCategoryIdAndOrganizationId(categoryId, orgId)).thenReturn(Optional.of(category));

        Employee emp = eligibleEmployee();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));

        when(loanApplicationRepository.findBlockingApplications(eq(orgId), eq(employeeId), anyList(), eq(applicationId)))
                .thenReturn(List.of());

        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = loanApplicationService.submit(orgId, applicationId, UUID.randomUUID());

        assertEquals(LoanApplicationStatus.SUBMITTED, dto.getStatus());
        verify(loanApplicationActionRepository).save(any(LoanApplicationAction.class));
    }

    private LoanApplication draftApplication() {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(applicationId);
        app.setOrganizationId(orgId);
        app.setEmployeeId(employeeId);
        app.setCategoryId(categoryId);
        app.setRequestedAmount(new BigDecimal("10000.00"));
        app.setRequestedTenureMonths(12);
        app.setStatus(LoanApplicationStatus.DRAFT);
        app.setApplicationDate(LocalDate.now());
        return app;
    }

    private LoanOrganizationSettingsDto settingsDto() {
        return LoanOrganizationSettingsDto.builder()
                .organizationId(orgId)
                .minTenureMonths(6)
                .maxPrincipalAmount(new BigDecimal("150000.00"))
                .currency("BDT")
                .enforceSingleActiveLoan(true)
                .allowSalaryAdvanceWithActiveTermLoan(false)
                .disqualifyingEmploymentStatuses(List.of())
                .settlementAllocationPriority(List.of("PF_SETTLEMENT", "FINAL_SALARY", "OTHER_DUES"))
                .enforceSettlementAllocationOrder(true)
                .skipFinanceApproval(false)
                .salaryAdvanceSkipFinanceApproval(false)
                .build();
    }

    private LoanCategory activeCategory(LoanCategoryType type) {
        LoanCategory c = new LoanCategory();
        c.setCategoryId(categoryId);
        c.setOrganizationId(orgId);
        c.setCategoryType(type);
        c.setIsActive(true);
        return c;
    }

    private Employee eligibleEmployee() {
        Employee emp = new Employee();
        emp.setEmployeeId(employeeId);
        emp.setOrganizationId(orgId);
        emp.setHireDate(LocalDate.now().minusMonths(7));
        emp.setIsActive(true);
        emp.setEmploymentStatus("ACTIVE");
        return emp;
    }
}
