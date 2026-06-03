package com.easyops.hr.integration;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import com.easyops.hr.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Provident Fund features
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProvidentFundIntegrationTest {
    
    @Autowired
    private ProvidentFundService providentFundService;
    
    @Autowired
    private PayrollIntegrationService payrollIntegrationService;
    
    @Autowired
    private AccountingFinanceIntegrationService accountingFinanceIntegrationService;
    
    @Autowired
    private EpfAccountRepository epfAccountRepository;
    
    @Autowired
    private EpfContributionRepository epfContributionRepository;
    
    private UUID organizationId;
    private UUID employeeId;
    private UUID epfAccountId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }
    
    @Test
    void testCompleteEpfWorkflow() {
        // Step 1: Create EPF account
        EpfAccount account = EpfAccount.builder()
                .employeeId(employeeId)
                .organizationId(organizationId)
                .epfAccountNumber("EPF-" + employeeId.toString().substring(0, 8))
                .openingDate(LocalDate.now())
                .isActive(true)
                .build();
        
        EpfAccount created = providentFundService.createEpfAccount(account);
        epfAccountId = created.getEpfAccountId();
        assertNotNull(epfAccountId);
        
        // Step 2: Create contribution
        EpfContribution contribution = EpfContribution.builder()
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .organizationId(organizationId)
                .contributionMonth(LocalDate.now().getMonthValue())
                .contributionYear(LocalDate.now().getYear())
                .employeeBasicSalary(new BigDecimal("50000"))
                .employeeContributionRate(new BigDecimal("12.00"))
                .employerContributionRate(new BigDecimal("12.00"))
                .build();
        
        EpfContribution saved = providentFundService.createContribution(contribution);
        assertNotNull(saved.getContributionId());
        assertTrue(saved.getTotalContribution().compareTo(BigDecimal.ZERO) > 0);
        
        // Step 3: Calculate interest
        String financialYear = LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() + 1);
        EpfInterestCalculation interest = providentFundService.calculateInterest(
                epfAccountId, financialYear, new BigDecimal("8.50"));
        assertNotNull(interest);
        
        // Step 4: Integration with payroll
        UUID payrollRunId = UUID.randomUUID();
        payrollIntegrationService.processEpfContributionsForPayroll(
                payrollRunId, organizationId, 
                LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        
        // Verify contribution is linked to payroll
        EpfContribution updated = epfContributionRepository.findById(saved.getContributionId())
                .orElseThrow();
        assertEquals(payrollRunId, updated.getPayrollRunId());
    }
    
    @Test
    void testEpfWithdrawalWorkflow() {
        // Create account and contribution first
        EpfAccount account = EpfAccount.builder()
                .employeeId(employeeId)
                .organizationId(organizationId)
                .epfAccountNumber("EPF-TEST")
                .openingDate(LocalDate.now())
                .currentBalance(new BigDecimal("100000"))
                .build();
        EpfAccount created = providentFundService.createEpfAccount(account);
        
        // Create withdrawal request
        EpfWithdrawal withdrawal = EpfWithdrawal.builder()
                .epfAccountId(created.getEpfAccountId())
                .employeeId(employeeId)
                .organizationId(organizationId)
                .withdrawalType("partial")
                .requestDate(LocalDate.now())
                .requestedAmount(new BigDecimal("50000"))
                .reason("Medical emergency")
                .build();
        
        EpfWithdrawal requested = providentFundService.createWithdrawalRequest(withdrawal);
        assertEquals("pending", requested.getStatus());
        
        // Approve withdrawal
        EpfWithdrawal approved = providentFundService.approveWithdrawalRequest(
                requested.getWithdrawalId(), UUID.randomUUID());
        assertEquals("approved", approved.getStatus());
        
        // Process withdrawal
        EpfWithdrawal processed = providentFundService.processWithdrawal(
                approved.getWithdrawalId(), "PAY-REF-001");
        assertEquals("processed", processed.getStatus());
    }
}

