package com.easyops.hr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CustomReportBuilderServiceTest {
    
    @InjectMocks
    private CustomReportBuilderService customReportBuilderService;
    
    private UUID organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }
    
    @Test
    void testBuildCustomReport_Success() {
        // Given
        Map<String, Object> reportConfig = new HashMap<>();
        reportConfig.put("reportType", "salary_summary");
        reportConfig.put("organizationId", organizationId.toString());
        reportConfig.put("dataSource", "payroll");
        reportConfig.put("filters", Map.of("month", 1, "year", 2024));
        
        // When
        Map<String, Object> result = customReportBuilderService.buildCustomReport(reportConfig);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("data"));
    }
    
    @Test
    void testGetAvailableReportTypes_Success() {
        // When
        List<Map<String, Object>> result = customReportBuilderService.getAvailableReportTypes();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}

