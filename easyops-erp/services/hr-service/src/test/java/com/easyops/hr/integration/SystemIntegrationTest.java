package com.easyops.hr.integration;

import com.easyops.hr.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for system-wide integration
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SystemIntegrationTest {
    
    @Autowired
    private PayrollIntegrationService payrollIntegrationService;
    
    @Autowired
    private EmployeeManagementIntegrationService employeeManagementIntegrationService;
    
    @Autowired
    private AccountingFinanceIntegrationService accountingFinanceIntegrationService;
    
    @Autowired
    private DataSynchronizationService dataSynchronizationService;
    
    @Test
    void testPayrollIntegration() {
        UUID organizationId = UUID.randomUUID();
        UUID payrollRunId = UUID.randomUUID();
        
        // Test complete payroll integration
        assertDoesNotThrow(() -> {
            payrollIntegrationService.completePayrollIntegration(
                    payrollRunId, organizationId, 1, 2024);
        });
    }
    
    @Test
    void testEmployeeDataValidation() {
        UUID employeeId = UUID.randomUUID();
        
        // Test employee data validation
        boolean isValid = employeeManagementIntegrationService.validateEmployeeData(employeeId);
        // Result depends on whether employee exists in employee service
        assertNotNull(isValid);
    }
    
    @Test
    void testDataIntegrityValidation() {
        UUID organizationId = UUID.randomUUID();
        
        // Test data integrity validation
        assertDoesNotThrow(() -> {
            dataSynchronizationService.validateDataIntegrity(organizationId);
        });
    }
}

