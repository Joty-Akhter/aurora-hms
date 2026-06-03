package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional bridge to an external <strong>payroll-service</strong> (HTTP). Canonical payroll and statutory
 * calculations run in {@code hr-service}; when {@code hr.integration.external-payroll-service.enabled} is false
 * (default), no outbound calls are made — only in-DB links such as {@code EpfContribution.payrollRunId} are updated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollIntegrationService {
    
    private final EpfContributionRepository epfContributionRepository;
    private final RestTemplate restTemplate;
    
    @Value("${services.payroll.base-url:http://payroll-service/api}")
    private String payrollServiceBaseUrl;

    /**
     * When false (default), payroll runs entirely in hr-service; no HTTP calls to a separate payroll microservice.
     * Set true only if you deploy {@code services.payroll.base-url} and implement those endpoints.
     */
    @Value("${hr.integration.external-payroll-service.enabled:false}")
    private boolean externalPayrollServiceEnabled;
    
    /**
     * Interface for integrating EPF contributions with payroll run
     * @param payrollRunId The payroll run ID
     * @param organizationId The organization ID
     * @param month The contribution month
     * @param year The contribution year
     */
    public void processEpfContributionsForPayroll(UUID payrollRunId, UUID organizationId, 
                                                  Integer month, Integer year) {
        log.info("Processing EPF contributions for payroll run: {}, period: {}/{}", 
                payrollRunId, month, year);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationAndPeriod(organizationId, month, year);
        
        for (EpfContribution contribution : contributions) {
            contribution.setPayrollRunId(payrollRunId);
            epfContributionRepository.save(contribution);

            if (!externalPayrollServiceEnabled) {
                continue;
            }
            try {
                Map<String, Object> payrollDetail = new HashMap<>();
                payrollDetail.put("payrollRunId", payrollRunId);
                payrollDetail.put("employeeId", contribution.getEmployeeId());
                payrollDetail.put("epfEmployeeContribution", contribution.getEmployeeContributionAmount());
                payrollDetail.put("epfEmployerContribution", contribution.getEmployerContributionAmount());

                String url = payrollServiceBaseUrl + "/payroll-runs/" + payrollRunId + "/details/epf";
                restTemplate.postForObject(url, payrollDetail, Map.class);
            } catch (Exception e) {
                log.error("Error updating payroll detail with EPF for employee: {}",
                        contribution.getEmployeeId(), e);
            }
        }
        
        log.info("Processed {} EPF contributions for payroll run: {}", contributions.size(), payrollRunId);
    }
    
    /**
     * Get EPF contribution amount for an employee for a specific period
     * @param employeeId The employee ID
     * @param month The contribution month
     * @param year The contribution year
     * @return Total EPF contribution amount (employee + employer)
     */
    public Double getEpfContributionAmount(UUID employeeId, Integer month, Integer year) {
        log.debug("Getting EPF contribution amount for employee: {}, period: {}/{}", 
                employeeId, month, year);
        
        List<EpfContribution> contributions = epfContributionRepository.findByEmployeeId(employeeId).stream()
                .filter(c -> c.getContributionMonth().equals(month) && 
                        c.getContributionYear().equals(year))
                .toList();
        
        BigDecimal total = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.doubleValue();
    }
    
    /**
     * Complete payroll integration - processes EPF contributions
     */
    public void completePayrollIntegration(UUID payrollRunId, UUID organizationId, 
                                         Integer month, Integer year) {
        log.info("Completing full payroll integration for payroll run: {}, period: {}/{}", 
                payrollRunId, month, year);
        
        // Process EPF contributions
        processEpfContributionsForPayroll(payrollRunId, organizationId, month, year);
        
        if (!externalPayrollServiceEnabled) {
            log.debug("External payroll integration disabled; skipping integration-status callback");
            return;
        }
        try {
            Map<String, Object> integrationStatus = new HashMap<>();
            integrationStatus.put("payrollRunId", payrollRunId);
            integrationStatus.put("epfIntegrated", true);
            integrationStatus.put("integrationDate", LocalDate.now());

            String url = payrollServiceBaseUrl + "/payroll-runs/" + payrollRunId + "/integration-status";
            restTemplate.put(url, integrationStatus);
        } catch (Exception e) {
            log.error("Error updating payroll integration status", e);
        }
    }
}

