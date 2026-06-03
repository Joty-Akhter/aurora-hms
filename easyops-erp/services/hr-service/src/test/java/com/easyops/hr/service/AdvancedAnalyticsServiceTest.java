package com.easyops.hr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AdvancedAnalyticsServiceTest {
    
    @InjectMocks
    private AdvancedAnalyticsService analyticsService;
    
    private UUID organizationId;
    
    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }
    
    @Test
    void testForecastProvidentFundParticipation_Success() {
        Map<String, Object> result =
                analyticsService.forecastProvidentFundParticipation(organizationId, 12);
        
        assertNotNull(result);
        assertTrue(result.containsKey("forecastedParticipation"));
    }
    
    @Test
    void testAnalyzeTrendsAndPatterns_Success() {
        String entityType = "provident_fund";
        
        Map<String, Object> result =
                analyticsService.analyzeTrendsAndPatterns(organizationId, entityType, 12);
        
        assertNotNull(result);
        assertTrue(result.containsKey("monthlyTrend"));
    }
}

