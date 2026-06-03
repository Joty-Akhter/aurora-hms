package com.easyops.hr.service;

import com.easyops.hr.entity.ScheduledReport;
import com.easyops.hr.repository.ScheduledReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledReportingServiceTest {
    
    @Mock
    private ScheduledReportRepository scheduledReportRepository;
    
    @InjectMocks
    private ScheduledReportingService scheduledReportingService;
    
    private UUID organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }
    
    @Test
    void testCreateScheduledReport_Success() {
        // Given
        ScheduledReport report = ScheduledReport.builder()
                .organizationId(organizationId)
                .reportName("Monthly Payroll Report")
                .reportType("salary_summary")
                .scheduleFrequency("monthly")
                .isActive(true)
                .build();
        
        when(scheduledReportRepository.save(any(ScheduledReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ScheduledReport result = scheduledReportingService.createScheduledReport(report);
        
        // Then
        assertNotNull(result);
        assertEquals("Monthly Incentive Report", result.getReportName());
        verify(scheduledReportRepository, times(1)).save(any(ScheduledReport.class));
    }
    
    @Test
    void testGetScheduledReports_Success() {
        // Given
        ScheduledReport report = ScheduledReport.builder()
                .organizationId(organizationId)
                .reportName("Test Report")
                .build();
        
        when(scheduledReportRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(report));
        
        // When
        List<ScheduledReport> result = scheduledReportingService.getScheduledReports(organizationId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    void testExecuteReport_Success() {
        // Given
        UUID reportId = UUID.randomUUID();
        ScheduledReport report = ScheduledReport.builder()
                .scheduledReportId(reportId)
                .organizationId(organizationId)
                .isActive(true)
                .build();
        
        when(scheduledReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(scheduledReportRepository.save(any(ScheduledReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        scheduledReportingService.executeReport(report);
        
        // Then
        verify(scheduledReportRepository, atLeastOnce()).save(any(ScheduledReport.class));
    }
}

