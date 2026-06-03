package com.easyops.hr.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.easyops.hr.entity.EpfAccount;
import com.easyops.hr.repository.EpfAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration service for Employee Management system
 * Handles synchronization of employee data for Provident Fund calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeManagementIntegrationService {
    
    private final EpfAccountRepository epfAccountRepository;
    private final RestTemplate restTemplate;
    
    @Value("${services.employee.base-url:http://employee-service/api}")
    private String employeeServiceBaseUrl;
    
    /**
     * Sync employee data for Provident Fund eligibility
     */
    public void syncEmployeeDataForProvidentFund(UUID organizationId) {
        log.info("Syncing employee data for Provident Fund eligibility for organization: {}", organizationId);
        
        try {
            // Fetch employees from employee service
            String url = employeeServiceBaseUrl + "/employees?organizationId=" + organizationId;
            List<Map<String, Object>> employees = restTemplate.getForObject(url, List.class);
            
            if (employees != null) {
                for (Map<String, Object> employeeData : employees) {
                    UUID employeeId = UUID.fromString(employeeData.get("employeeId").toString());
                    
                    // Check if EPF account exists
                    List<EpfAccount> accounts = epfAccountRepository.findByEmployeeId(employeeId);
                    if (accounts.isEmpty()) {
                        // Employee is eligible but doesn't have EPF account
                        log.debug("Employee {} is eligible for EPF but doesn't have account", employeeId);
                    }
                }
            }
            
            log.info("Successfully synced employee data for Provident Fund");
        } catch (Exception e) {
            log.error("Error syncing employee data for Provident Fund", e);
            throw new RuntimeException("Failed to sync employee data", e);
        }
    }
    
    /**
     * Get employee details for calculations
     */
    public Map<String, Object> getEmployeeDetails(UUID employeeId) {
        log.debug("Getting employee details for: {}", employeeId);
        
        try {
            String url = employeeServiceBaseUrl + "/employees/" + employeeId;
            Map<String, Object> employee = restTemplate.getForObject(url, Map.class);
            return employee != null ? employee : new HashMap<>();
        } catch (Exception e) {
            log.error("Error getting employee details", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Validate employee data before calculations
     */
    public boolean validateEmployeeData(UUID employeeId) {
        log.debug("Validating employee data for: {}", employeeId);
        
        Map<String, Object> employee = getEmployeeDetails(employeeId);
        
        // Validate required fields
        if (employee.isEmpty()) {
            return false;
        }
        
        // Check if employee is active
        Object status = employee.get("status");
        if (status == null || !"ACTIVE".equals(status.toString())) {
            return false;
        }
        
        // Check if employee has required information
        return employee.containsKey("employeeId") && 
               employee.containsKey("organizationId");
    }
}

