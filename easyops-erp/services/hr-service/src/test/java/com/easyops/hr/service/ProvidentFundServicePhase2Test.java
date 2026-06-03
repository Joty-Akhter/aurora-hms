package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidentFundServicePhase2Test {
    
    @Mock
    private EpfAccountRepository epfAccountRepository;
    
    @Mock
    private EpfContributionRepository epfContributionRepository;
    
    @Mock
    private EpfInterestCalculationRepository epfInterestCalculationRepository;
    
    @Mock
    private EpfWithdrawalRepository epfWithdrawalRepository;
    
    @Mock
    private EpfTransferRepository epfTransferRepository;
    
    @Mock
    private EpfNominationRepository epfNominationRepository;
    
    @Mock
    private EpfComplianceRecordRepository epfComplianceRecordRepository;
    
    @InjectMocks
    private ProvidentFundService providentFundService;
    
    private UUID organizationId;
    private UUID employeeId;
    private UUID epfAccountId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        epfAccountId = UUID.randomUUID();
    }
    
    @Test
    void testCalculateInterestForFinancialYear_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .organizationId(organizationId)
                .employeeId(employeeId)
                .currentBalance(new BigDecimal("100000"))
                .interestBalance(BigDecimal.ZERO)
                .build();
        
        EpfContribution contribution = EpfContribution.builder()
                .contributionMonth(4)
                .contributionYear(2024)
                .totalContribution(new BigDecimal("5000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        when(epfInterestCalculationRepository.findByEpfAccountIdAndFinancialYear(epfAccountId, 2024))
                .thenReturn(Optional.empty());
        when(epfInterestCalculationRepository.findByEpfAccountIdAndFinancialYear(epfAccountId, 2023))
                .thenReturn(Optional.empty());
        when(epfContributionRepository.findByEpfAccountId(epfAccountId))
                .thenReturn(List.of(contribution));
        when(epfInterestCalculationRepository.save(any(EpfInterestCalculation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(epfAccountRepository.save(any(EpfAccount.class))).thenReturn(account);
        
        // When
        EpfInterestCalculation result = providentFundService.calculateInterestForFinancialYear(
                epfAccountId, 2024, new BigDecimal("8.15"));
        
        // Then
        assertNotNull(result);
        assertEquals(2024, result.getFinancialYear());
        assertNotNull(result.getInterestAmount());
        assertTrue(result.getInterestAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(epfInterestCalculationRepository, times(1)).save(any(EpfInterestCalculation.class));
    }
    
    @Test
    void testCreateWithdrawalRequest_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .organizationId(organizationId)
                .employeeId(employeeId)
                .currentBalance(new BigDecimal("100000"))
                .build();
        
        EpfWithdrawal withdrawal = EpfWithdrawal.builder()
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .organizationId(organizationId)
                .withdrawalType("partial")
                .requestedAmount(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        when(epfWithdrawalRepository.save(any(EpfWithdrawal.class))).thenReturn(withdrawal);
        
        // When
        EpfWithdrawal result = providentFundService.createWithdrawalRequest(withdrawal);
        
        // Then
        assertNotNull(result);
        assertEquals("pending", result.getStatus());
        verify(epfWithdrawalRepository, times(1)).save(withdrawal);
    }
    
    @Test
    void testCreateWithdrawalRequest_ExceedsBalance() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("10000"))
                .build();
        
        EpfWithdrawal withdrawal = EpfWithdrawal.builder()
                .epfAccountId(epfAccountId)
                .requestedAmount(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            providentFundService.createWithdrawalRequest(withdrawal);
        });
    }
    
    @Test
    void testApproveWithdrawal_Success() {
        // Given
        EpfWithdrawal withdrawal = EpfWithdrawal.builder()
                .withdrawalId(UUID.randomUUID())
                .epfAccountId(epfAccountId)
                .status("pending")
                .requestedAmount(new BigDecimal("50000"))
                .build();
        
        when(epfWithdrawalRepository.findById(withdrawal.getWithdrawalId()))
                .thenReturn(Optional.of(withdrawal));
        when(epfWithdrawalRepository.save(any(EpfWithdrawal.class))).thenReturn(withdrawal);
        
        // When
        EpfWithdrawal result = providentFundService.approveWithdrawal(
                withdrawal.getWithdrawalId(), UUID.randomUUID(), null);
        
        // Then
        assertNotNull(result);
        assertEquals("approved", result.getStatus());
        verify(epfWithdrawalRepository, times(1)).save(withdrawal);
    }
    
    @Test
    void testProcessTransfer_Success() {
        // Given
        EpfAccount sourceAccount = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("100000"))
                .build();
        
        UUID targetAccountId = UUID.randomUUID();
        EpfAccount targetAccount = EpfAccount.builder()
                .epfAccountId(targetAccountId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        UUID transferId = UUID.randomUUID();
        EpfTransfer transfer = EpfTransfer.builder()
                .transferId(transferId)
                .sourceEpfAccountId(epfAccountId)
                .targetEpfAccountId(targetAccountId)
                .transferAmount(new BigDecimal("30000"))
                .status("pending")
                .build();
        
        when(epfTransferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(sourceAccount));
        when(epfAccountRepository.findById(targetAccountId)).thenReturn(Optional.of(targetAccount));
        when(epfTransferRepository.save(any(EpfTransfer.class))).thenReturn(transfer);
        when(epfAccountRepository.save(any(EpfAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        EpfTransfer result = providentFundService.processTransfer(transferId);
        
        // Then
        assertNotNull(result);
        assertEquals("processed", result.getStatus());
        assertNotNull(result.getTransferDate());
        verify(epfAccountRepository, times(2)).save(any(EpfAccount.class));
    }
    
    @Test
    void testCreateNomination_Success() {
        // Given
        UUID nominationId = UUID.randomUUID();
        EpfNomination nomination = EpfNomination.builder()
                .nominationId(nominationId)
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .organizationId(organizationId)
                .nomineeName("John Doe")
                .nomineeRelationship("Spouse")
                .sharePercentage(new BigDecimal("100.00"))
                .isPrimary(true)
                .build();
        
        when(epfNominationRepository.findByEpfAccountIdAndIsActive(epfAccountId, true))
                .thenReturn(List.of());
        when(epfNominationRepository.findByEpfAccountIdAndIsPrimary(epfAccountId, true))
                .thenReturn(Optional.empty());
        when(epfNominationRepository.save(any(EpfNomination.class))).thenReturn(nomination);
        
        // When
        EpfNomination result = providentFundService.createNomination(nomination);
        
        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getNomineeName());
        verify(epfNominationRepository, times(1)).save(nomination);
    }
    
    @Test
    void testGenerateAccountStatement_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("100000"))
                .build();
        
        EpfContribution contribution = EpfContribution.builder()
                .contributionMonth(1)
                .contributionYear(2024)
                .totalContribution(new BigDecimal("5000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        when(epfContributionRepository.findByEpfAccountId(epfAccountId))
                .thenReturn(List.of(contribution));
        when(epfWithdrawalRepository.findByEpfAccountId(epfAccountId))
                .thenReturn(List.of());
        when(epfTransferRepository.findBySourceEpfAccountId(epfAccountId))
                .thenReturn(List.of());
        
        // When
        java.util.Map<String, Object> statement = providentFundService.generateAccountStatement(
                epfAccountId, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        
        // Then
        assertNotNull(statement);
        assertTrue(statement.containsKey("account"));
        assertTrue(statement.containsKey("contributions"));
        assertTrue(statement.containsKey("closingBalance"));
    }
    
    @Test
    void testCreateComplianceRecord_Success() {
        // Given
        EpfComplianceRecord record = EpfComplianceRecord.builder()
                .organizationId(organizationId)
                .complianceType("monthly_return")
                .compliancePeriodStart(LocalDate.of(2024, 1, 1))
                .compliancePeriodEnd(LocalDate.of(2024, 1, 31))
                .dueDate(LocalDate.of(2024, 2, 15))
                .status("pending")
                .build();
        
        when(epfComplianceRecordRepository.save(any(EpfComplianceRecord.class))).thenReturn(record);
        
        // When
        EpfComplianceRecord result = providentFundService.createComplianceRecord(record);
        
        // Then
        assertNotNull(result);
        assertEquals("monthly_return", result.getComplianceType());
        verify(epfComplianceRecordRepository, times(1)).save(record);
    }
    
    @Test
    void testCalculateStatutoryContribution_Success() {
        // Given
        EpfContribution contribution1 = EpfContribution.builder()
                .totalContribution(new BigDecimal("5000"))
                .build();
        
        EpfContribution contribution2 = EpfContribution.builder()
                .totalContribution(new BigDecimal("3000"))
                .build();
        
        when(epfContributionRepository.findByOrganizationAndPeriod(organizationId, 1, 2024))
                .thenReturn(List.of(contribution1, contribution2));
        
        // When
        BigDecimal result = providentFundService.calculateStatutoryContribution(organizationId, 1, 2024);
        
        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("8000"), result);
    }
}

