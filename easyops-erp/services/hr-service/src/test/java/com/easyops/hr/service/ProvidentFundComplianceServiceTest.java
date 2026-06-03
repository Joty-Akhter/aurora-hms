package com.easyops.hr.service;

import com.easyops.hr.entity.EpfComplianceRecord;
import com.easyops.hr.repository.EpfComplianceRecordRepository;
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
class ProvidentFundComplianceServiceTest {
    
    @Mock
    private EpfComplianceRecordRepository complianceRecordRepository;
    
    @InjectMocks
    private ProvidentFundComplianceService complianceService;
    
    private UUID organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }
    
    @Test
    void testCheckCompliance_Success() {
        // Given
        EpfComplianceRecord record = EpfComplianceRecord.builder()
                .organizationId(organizationId)
                .complianceType("contribution")
                .amount(new BigDecimal("10000"))
                .status("verified")
                .build();
        
        when(complianceRecordRepository.findByOrganizationIdAndComplianceMonthAndComplianceYear(
                organizationId, 1, 2024)).thenReturn(List.of(record));
        
        // When
        Map<String, Object> result = complianceService.checkCompliance(organizationId, 1, 2024);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("overallCompliant"));
    }
    
    @Test
    void testAutomateStatutoryCompliance_Success() {
        // Given
        when(complianceRecordRepository.save(any(EpfComplianceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        complianceService.automateStatutoryCompliance(organizationId, 1, 2024);
        
        // Then
        verify(complianceRecordRepository, atLeastOnce()).save(any(EpfComplianceRecord.class));
    }
    
    @Test
    void testCalculatePenalties_Success() {
        // Given
        EpfComplianceRecord nonCompliant = EpfComplianceRecord.builder()
                .organizationId(organizationId)
                .status("rejected")
                .amount(new BigDecimal("10000"))
                .build();
        
        when(complianceRecordRepository.findByOrganizationIdAndComplianceMonthAndComplianceYear(
                organizationId, 1, 2024)).thenReturn(List.of(nonCompliant));
        
        // When
        Map<String, Object> result = complianceService.calculatePenalties(organizationId, 1, 2024);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalPenalty"));
    }
}

