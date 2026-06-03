package com.easyops.pharma.service;

import com.easyops.pharma.client.HrClient;
import com.easyops.pharma.entity.IncentiveDistribution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for integrating with HR Service
 * Handles employee data synchronization and incentive payment processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HrIntegrationService {
    
    private final HrClient hrClient;
    
    /**
     * Sync employee data from HR module
     * Fetches latest employee information
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "employees", key = "#employeeId")
    public Map<String, Object> getEmployeeData(UUID employeeId) {
        try {
            log.debug("Fetching employee data from HR service: {}", employeeId);
            Map<String, Object> employee = hrClient.getEmployee(employeeId);
            return employee;
        } catch (Exception e) {
            log.error("Failed to fetch employee data from HR service: {}", employeeId, e);
            return null;
        }
    }
    
    /**
     * Refresh employee data cache
     */
    @CacheEvict(value = "employees", key = "#employeeId")
    public void refreshEmployeeCache(UUID employeeId) {
        log.debug("Refreshing employee cache: {}", employeeId);
    }
    
    /**
     * Create incentive bonus record in HR module for payroll processing
     * This allows HR to process incentive payments as part of payroll
     */
    @Transactional
    public void createIncentiveBonus(IncentiveDistribution distribution, Integer year, Integer month) {
        try {
            log.info("Creating incentive bonus in HR module for employee: {}, amount: {}", 
                    distribution.getEmployeeId(), distribution.getIncentiveAmount());
            
            Map<String, Object> bonusRequest = new HashMap<>();
            bonusRequest.put("employeeId", distribution.getEmployeeId());
            
            UUID orgId = null;
            if (distribution.getIncentiveCalculation() != null) {
                orgId = distribution.getIncentiveCalculation().getOrganizationId();
            }
            if (orgId == null) {
                orgId = getEmployeeOrgId(distribution.getEmployeeId());
            }
            bonusRequest.put("organizationId", orgId);
            bonusRequest.put("bonusType", "PHARMA_INCENTIVE");
            bonusRequest.put("amount", distribution.getIncentiveAmount());
            bonusRequest.put("currency", "BDT");
            bonusRequest.put("bonusPeriod", year + "-" + String.format("%02d", month));
            bonusRequest.put("description", "Pharma Incentive - Territory: " + distribution.getTerritoryId() + 
                    ", Role: " + distribution.getRoleInArea());
            bonusRequest.put("status", "approved"); // Auto-approved as already calculated
            bonusRequest.put("isTaxable", true);
            
            Map<String, Object> result = hrClient.createBonus(bonusRequest);
            log.info("Successfully created incentive bonus in HR module. Bonus ID: {}", result.get("bonusId"));
            
        } catch (Exception e) {
            log.error("Failed to create incentive bonus in HR module for employee: {}", 
                    distribution.getEmployeeId(), e);
            // Don't throw exception - log error but don't block incentive calculation
        }
    }
    
    /**
     * Get employee organization ID (helper method)
     */
    private UUID getEmployeeOrgId(UUID employeeId) {
        try {
            Map<String, Object> employee = getEmployeeData(employeeId);
            if (employee != null && employee.get("organizationId") != null) {
                return UUID.fromString(employee.get("organizationId").toString());
            }
        } catch (Exception e) {
            log.warn("Could not get organization ID for employee: {}", employeeId, e);
        }
        return null;
    }
    
    /**
     * Batch create incentive bonuses for all distributions
     */
    @Transactional
    public void createIncentiveBonuses(List<IncentiveDistribution> distributions, Integer year, Integer month) {
        log.info("Creating incentive bonuses for {} distributions", distributions.size());
        for (IncentiveDistribution distribution : distributions) {
            if (distribution.getIncentiveAmount().compareTo(BigDecimal.ZERO) > 0) {
                createIncentiveBonus(distribution, year, month);
            }
        }
    }
    
    /**
     * Get employee assignments from HR module
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmployeeAssignments(UUID employeeId) {
        try {
            log.debug("Fetching employee assignments from HR service: {}", employeeId);
            return hrClient.getEmployeeAssignments(employeeId);
        } catch (Exception e) {
            log.error("Failed to fetch employee assignments from HR service: {}", employeeId, e);
            return List.of();
        }
    }
}
