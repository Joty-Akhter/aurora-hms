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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidentFundReportingServiceTest {
    
    @Mock
    private EpfAccountRepository epfAccountRepository;
    
    @Mock
    private EpfContributionRepository epfContributionRepository;
    
    @InjectMocks
    private ProvidentFundReportingService reportingService;
    
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
    void testGetExecutiveDashboard_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("100000"))
                .build();
        
        when(epfAccountRepository.findByOrganizationId(organizationId)).thenReturn(List.of(account));
        
        // When
        Map<String, Object> result = reportingService.getExecutiveDashboard(organizationId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalBalance"));
    }
    
    @Test
    void testGetEmployeeStatement_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusMonths(6);
        LocalDate endDate = LocalDate.now();
        
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        EpfContribution contribution = EpfContribution.builder()
                .totalContribution(new BigDecimal("5000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(java.util.Optional.of(account));
        when(epfContributionRepository.findByEpfAccountIdAndContributionDateBetween(
                epfAccountId, startDate, endDate)).thenReturn(List.of(contribution));
        
        // When
        Map<String, Object> result = reportingService.getEmployeeStatement(
                employeeId, epfAccountId, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("openingBalance"));
        assertTrue(result.containsKey("closingBalance"));
    }
    
    @Test
    void testGetComplianceReport_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusMonths(12);
        LocalDate endDate = LocalDate.now();
        
        // When
        Map<String, Object> result = reportingService.getComplianceReport(
                organizationId, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("complianceRate"));
    }
}

