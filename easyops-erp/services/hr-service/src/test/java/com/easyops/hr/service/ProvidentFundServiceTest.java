package com.easyops.hr.service;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.entity.EpfContribution;
import com.easyops.hr.repository.EpfAccountRepository;
import com.easyops.hr.repository.EpfContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidentFundServiceTest {
    
    @Mock
    private EpfAccountRepository epfAccountRepository;
    
    @Mock
    private EpfContributionRepository epfContributionRepository;
    
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
    void testCreateEpfAccount_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .organizationId(organizationId)
                .employeeId(employeeId)
                .epfAccountNumber("EPF001")
                .openingDate(LocalDate.now())
                .build();
        
        when(epfAccountRepository.findByOrganizationIdAndEpfAccountNumber(
                organizationId, "EPF001")).thenReturn(Optional.empty());
        when(epfAccountRepository.save(any(EpfAccount.class))).thenReturn(account);
        
        // When
        EpfAccount result = providentFundService.createEpfAccount(account);
        
        // Then
        assertNotNull(result);
        assertEquals("EPF001", result.getEpfAccountNumber());
        verify(epfAccountRepository, times(1)).save(account);
    }
    
    @Test
    void testCreateEpfAccount_DuplicateAccountNumber() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .organizationId(organizationId)
                .employeeId(employeeId)
                .epfAccountNumber("EPF001")
                .openingDate(LocalDate.now())
                .build();
        
        when(epfAccountRepository.findByOrganizationIdAndEpfAccountNumber(
                organizationId, "EPF001")).thenReturn(Optional.of(account));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            providentFundService.createEpfAccount(account);
        });
    }
    
    @Test
    void testCreateContribution_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .organizationId(organizationId)
                .employeeId(employeeId)
                .build();
        
        EpfContribution contribution = EpfContribution.builder()
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .organizationId(organizationId)
                .contributionMonth(1)
                .contributionYear(2024)
                .employeeBasicSalary(new BigDecimal("50000"))
                .employeeContributionRate(new BigDecimal("12.00"))
                .employerContributionRate(new BigDecimal("12.00"))
                .build();
        
        when(epfContributionRepository.findByEpfAccountIdAndContributionMonthAndContributionYear(
                epfAccountId, 1, 2024)).thenReturn(Optional.empty());
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        when(epfContributionRepository.findByEpfAccountId(epfAccountId))
                .thenReturn(java.util.Collections.emptyList());
        when(epfContributionRepository.save(any(EpfContribution.class))).thenReturn(contribution);
        when(epfAccountRepository.save(any(EpfAccount.class))).thenReturn(account);
        
        // When
        EpfContribution result = providentFundService.createContribution(contribution);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getEmployeeContributionAmount());
        assertNotNull(result.getEmployerContributionAmount());
        assertNotNull(result.getTotalContribution());
        verify(epfContributionRepository, times(1)).save(any(EpfContribution.class));
    }
}

