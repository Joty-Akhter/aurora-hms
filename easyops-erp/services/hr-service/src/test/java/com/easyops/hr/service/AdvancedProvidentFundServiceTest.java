package com.easyops.hr.service;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.repository.EpfAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedProvidentFundServiceTest {
    
    @Mock
    private EpfAccountRepository epfAccountRepository;
    
    @InjectMocks
    private AdvancedProvidentFundService advancedProvidentFundService;
    
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
    void testGetProvidentFundRecommendations_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findByEmployeeId(employeeId)).thenReturn(List.of(account));
        
        // When
        Map<String, Object> result = advancedProvidentFundService
                .getProvidentFundRecommendations(employeeId, organizationId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("recommendations"));
    }
    
    @Test
    void testOptimizeContributions_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        
        // When
        Map<String, Object> result = advancedProvidentFundService.optimizeContributions(epfAccountId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("optimizedContribution"));
    }
    
    @Test
    void testForecastProvidentFund_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        
        // When
        Map<String, Object> result = advancedProvidentFundService.forecastProvidentFund(epfAccountId, 12);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("projectedBalance"));
    }
    
    @Test
    void testAssessRisk_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(epfAccountId)
                .currentBalance(new BigDecimal("50000"))
                .build();
        
        when(epfAccountRepository.findById(epfAccountId)).thenReturn(Optional.of(account));
        
        // When
        Map<String, Object> result = advancedProvidentFundService.assessRisk(epfAccountId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("riskLevel"));
        assertTrue(result.containsKey("riskScore"));
    }
}

