package com.easyops.pharma.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for HR Service
 * Used for employee data synchronization and incentive payment processing
 */
@FeignClient(name = "hr-service")
public interface HrClient {
    
    /**
     * Get employee by ID
     */
    @GetMapping("/api/hr/employees/{id}")
    Map<String, Object> getEmployee(@PathVariable("id") UUID id);
    
    /**
     * Get all employees for organization
     */
    @GetMapping("/api/hr/employees")
    List<Map<String, Object>> getEmployees(@RequestParam("organizationId") UUID organizationId);
    
    /**
     * Get employee assignments/roles
     */
    @GetMapping("/api/hr/employees/{id}/assignments")
    List<Map<String, Object>> getEmployeeAssignments(@PathVariable("id") UUID id);
    
    /**
     * Create bonus/incentive record for employee (for payroll processing)
     */
    @PostMapping("/api/hr/compensation/bonuses")
    Map<String, Object> createBonus(@RequestBody Map<String, Object> bonusRequest);
    
    /**
     * Get employee payroll details
     */
    @GetMapping("/api/hr/employees/{id}/payroll")
    Map<String, Object> getEmployeePayroll(@PathVariable("id") UUID id);
}
