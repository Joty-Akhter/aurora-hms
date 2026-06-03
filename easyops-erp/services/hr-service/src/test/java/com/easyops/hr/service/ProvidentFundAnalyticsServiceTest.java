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
class ProvidentFundAnalyticsServiceTest {
    
    @Mock
    private EpfAccountRepository epfAccountRepository;
    
    @Mock
    private EpfContributionRepository epfContributionRepository;
    
    @InjectMocks
    private ProvidentFundAnalyticsService analyticsService;
    
    private UUID organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }
    
    @Test
    void testGetParticipationMetrics_Success() {
        // Given
        EpfAccount account = EpfAccount.builder()
                .epfAccountId(UUID.randomUUID())
                .isActive(true)
                .build();
        
        when(epfAccountRepository.findByOrganizationId(organizationId)).thenReturn(List.of(account));
        
        // When
        Map<String, Object> result = analyticsService.getParticipationMetrics(organizationId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalAccounts"));
        assertTrue(result.containsKey("participationRate"));
    }
    
    @Test
    void testAnalyzeContributions_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusMonths(6);
        LocalDate endDate = LocalDate.now();
        
        EpfContribution contribution = EpfContribution.builder()
                .totalContribution(new BigDecimal("10000"))
                .build();
        
        when(epfContributionRepository.findByOrganizationIdAndContributionDateBetween(
                organizationId, startDate, endDate)).thenReturn(List.of(contribution));
        
        // When
        Map<String, Object> result = analyticsService.analyzeContributions(
                organizationId, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalContributions"));
    }
    
    @Test
    void testAnalyzeCosts_Success() {
        // Given
        Integer year = 2024;
        EpfContribution contribution = EpfContribution.builder()
                .employerContributionAmount(new BigDecimal("5000"))
                .build();
        
        when(epfContributionRepository.findByOrganizationIdAndContributionYear(
                organizationId, year)).thenReturn(List.of(contribution));
        
        // When
        Map<String, Object> result = analyticsService.analyzeCosts(organizationId, year);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalCost"));
    }
    
    @Test
    void testMeasureROI_Success() {
        // Given
        Integer year = 2024;
        
        // When
        Map<String, Object> result = analyticsService.measureROI(organizationId, year);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("roi"));
    }
}

