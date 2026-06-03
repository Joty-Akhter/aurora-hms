package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProvidentFundComplianceService {
    
    private final EpfComplianceRecordRepository epfComplianceRecordRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfAccountRepository epfAccountRepository;
    private final EpfOrganizationPolicyRepository epfOrganizationPolicyRepository;

    /** Default when no {@link EpfOrganizationPolicy} row exists for the organization. */
    private static final BigDecimal DEFAULT_EMPLOYEE_EPF_RATE = new BigDecimal("12.00");
    private static final BigDecimal DEFAULT_EMPLOYER_EPF_RATE = new BigDecimal("12.00");
    private static final BigDecimal EMPLOYER_EPF_ADMIN_RATE = new BigDecimal("0.50");
    private static final BigDecimal EMPLOYER_EDLI_RATE = new BigDecimal("0.50");
    
    /**
     * Check EPF Act compliance for organization
     */
    public Map<String, Object> checkCompliance(UUID organizationId, Integer month, Integer year) {
        log.info("Checking EPF compliance for organization: {}, period: {}/{}", organizationId, month, year);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationAndPeriod(organizationId, month, year);
        
        Map<String, Object> compliance = new HashMap<>();
        compliance.put("organizationId", organizationId);
        compliance.put("period", month + "/" + year);
        compliance.put("totalContributions", contributions.size());
        
        // Check contribution rates
        long nonCompliantContributions = contributions.stream()
                .filter(c -> !isCompliantContribution(c))
                .count();
        
        compliance.put("nonCompliantContributions", nonCompliantContributions);
        compliance.put("complianceStatus", nonCompliantContributions == 0 ? "Compliant" : "Non-Compliant");
        compliance.put("compliancePercentage", calculateCompliancePercentage(contributions.size(), 
                (int) nonCompliantContributions));
        
        // Check filing status
        List<EpfComplianceRecord> records = epfComplianceRecordRepository
                .findByOrganizationId(organizationId).stream()
                .filter(r -> r.getCompliancePeriodStart().getMonthValue() == month &&
                        r.getCompliancePeriodStart().getYear() == year)
                .collect(Collectors.toList());
        
        compliance.put("filingStatus", records.isEmpty() ? "Not Filed" : 
                records.get(0).getStatus());
        compliance.put("dueDate", calculateDueDate(month, year));
        compliance.put("isOverdue", isOverdue(month, year));
        
        return compliance;
    }
    
    /**
     * Automate statutory contribution compliance
     */
    public void automateStatutoryCompliance(UUID organizationId, Integer month, Integer year) {
        log.info("Automating statutory compliance for organization: {}, period: {}/{}", 
                organizationId, month, year);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationAndPeriod(organizationId, month, year);
        
        for (EpfContribution contribution : contributions) {
            if (!isCompliantContribution(contribution)) {
                // Auto-correct contribution rates
                correctContributionRates(contribution);
            }
        }
        
        // Create compliance record
        createComplianceRecord(organizationId, month, year);
    }
    
    /**
     * Generate compliance report
     */
    public Map<String, Object> generateComplianceReport(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating compliance report for organization: {} from {} to {}", 
                organizationId, startDate, endDate);
        
        List<EpfComplianceRecord> records = epfComplianceRecordRepository
                .findByOrganizationId(organizationId).stream()
                .filter(r -> !r.getCompliancePeriodStart().isBefore(startDate) &&
                        !r.getCompliancePeriodStart().isAfter(endDate))
                .collect(Collectors.toList());
        
        Map<String, Object> report = new HashMap<>();
        report.put("organizationId", organizationId);
        report.put("period", startDate + " to " + endDate);
        report.put("totalRecords", records.size());
        report.put("filedRecords", records.stream().filter(r -> "filed".equals(r.getStatus())).count());
        report.put("pendingRecords", records.stream().filter(r -> "pending".equals(r.getStatus())).count());
        report.put("overdueRecords", records.stream().filter(r -> "overdue".equals(r.getStatus())).count());
        report.put("records", records);
        
        return report;
    }
    
    /**
     * Monitor compliance and generate alerts
     */
    public List<Map<String, Object>> monitorCompliance(UUID organizationId) {
        log.info("Monitoring compliance for organization: {}", organizationId);
        
        List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        
        // Check for overdue filings
        List<EpfComplianceRecord> overdueRecords = epfComplianceRecordRepository
                .findOverdueComplianceRecords(organizationId, LocalDate.now());
        
        for (EpfComplianceRecord record : overdueRecords) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "overdue_filing");
            alert.put("severity", "high");
            alert.put("message", "Compliance filing is overdue: " + record.getComplianceType());
            alert.put("dueDate", record.getDueDate());
            alert.put("recordId", record.getComplianceRecordId());
            alerts.add(alert);
        }
        
        // Check for upcoming due dates
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        List<EpfComplianceRecord> upcomingRecords = epfComplianceRecordRepository
                .findByOrganizationId(organizationId).stream()
                .filter(r -> r.getDueDate().isBefore(nextMonth) && 
                        r.getDueDate().isAfter(LocalDate.now()) &&
                        "pending".equals(r.getStatus()))
                .collect(Collectors.toList());
        
        for (EpfComplianceRecord record : upcomingRecords) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "upcoming_due_date");
            alert.put("severity", "medium");
            alert.put("message", "Compliance filing due soon: " + record.getComplianceType());
            alert.put("dueDate", record.getDueDate());
            alert.put("recordId", record.getComplianceRecordId());
            alerts.add(alert);
        }
        
        return alerts;
    }
    
    /**
     * Calculate penalties for non-compliance
     */
    public Map<String, Object> calculatePenalties(UUID organizationId, Integer month, Integer year) {
        log.info("Calculating penalties for organization: {}, period: {}/{}", organizationId, month, year);
        
        List<EpfComplianceRecord> records = epfComplianceRecordRepository
                .findByOrganizationId(organizationId).stream()
                .filter(r -> r.getCompliancePeriodStart().getMonthValue() == month &&
                        r.getCompliancePeriodStart().getYear() == year &&
                        "overdue".equals(r.getStatus()))
                .collect(Collectors.toList());
        
        Map<String, Object> penalties = new HashMap<>();
        BigDecimal totalPenalty = BigDecimal.ZERO;
        
        for (EpfComplianceRecord record : records) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    record.getDueDate(), LocalDate.now());
            BigDecimal penalty = calculatePenaltyAmount(record, daysOverdue);
            totalPenalty = totalPenalty.add(penalty);
        }
        
        penalties.put("totalPenalty", totalPenalty);
        penalties.put("overdueRecords", records.size());
        penalties.put("penaltyDetails", records.stream().map(r -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("complianceType", r.getComplianceType());
            detail.put("dueDate", r.getDueDate());
            detail.put("daysOverdue", java.time.temporal.ChronoUnit.DAYS.between(
                    r.getDueDate(), LocalDate.now()));
            detail.put("penalty", calculatePenaltyAmount(r, 
                    java.time.temporal.ChronoUnit.DAYS.between(r.getDueDate(), LocalDate.now())));
            return detail;
        }).collect(Collectors.toList()));
        
        return penalties;
    }
    
    private boolean isCompliantContribution(EpfContribution contribution) {
        BigDecimal[] expected = expectedRatesForOrganization(contribution.getOrganizationId());
        BigDecimal employeeRate = contribution.getEmployeeContributionRate();
        BigDecimal employerRate = contribution.getEmployerContributionRate();
        return employeeRate.compareTo(expected[0]) == 0 && employerRate.compareTo(expected[1]) == 0;
    }

    private BigDecimal[] expectedRatesForOrganization(UUID organizationId) {
        return epfOrganizationPolicyRepository.findByOrganizationId(organizationId)
                .map(p -> new BigDecimal[]{
                        p.getEmployeeContributionRate() != null ? p.getEmployeeContributionRate() : DEFAULT_EMPLOYEE_EPF_RATE,
                        p.getEmployerContributionRate() != null ? p.getEmployerContributionRate() : DEFAULT_EMPLOYER_EPF_RATE
                })
                .orElse(new BigDecimal[]{DEFAULT_EMPLOYEE_EPF_RATE, DEFAULT_EMPLOYER_EPF_RATE});
    }
    
    private void correctContributionRates(EpfContribution contribution) {
        BigDecimal[] expected = expectedRatesForOrganization(contribution.getOrganizationId());
        contribution.setEmployeeContributionRate(expected[0]);
        contribution.setEmployerContributionRate(expected[1]);
        BigDecimal base = contribution.getPfWageBase() != null ? contribution.getPfWageBase() : contribution.getEmployeeBasicSalary();
        BigDecimal employeeAmount = base.multiply(expected[0]).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        contribution.setEmployeeContributionAmount(employeeAmount);
        BigDecimal employerAmount = base.multiply(expected[1]).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        contribution.setEmployerContributionAmount(employerAmount);
        contribution.setTotalContribution(employeeAmount.add(employerAmount));
    }
    
    private void createComplianceRecord(UUID organizationId, Integer month, Integer year) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        LocalDate dueDate = periodEnd.plusDays(15); // Typically due 15 days after period end
        
        EpfComplianceRecord record = EpfComplianceRecord.builder()
                .organizationId(organizationId)
                .complianceType("monthly_return")
                .compliancePeriodStart(periodStart)
                .compliancePeriodEnd(periodEnd)
                .dueDate(dueDate)
                .status(isOverdue(month, year) ? "overdue" : "pending")
                .build();
        
        epfComplianceRecordRepository.save(record);
    }
    
    private LocalDate calculateDueDate(Integer month, Integer year) {
        LocalDate periodEnd = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        return periodEnd.plusDays(15);
    }
    
    private boolean isOverdue(Integer month, Integer year) {
        LocalDate dueDate = calculateDueDate(month, year);
        return LocalDate.now().isAfter(dueDate);
    }
    
    private BigDecimal calculateCompliancePercentage(int total, int nonCompliant) {
        if (total == 0) return BigDecimal.ZERO;
        return new BigDecimal(total - nonCompliant)
                .divide(new BigDecimal(total), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculatePenaltyAmount(EpfComplianceRecord record, long daysOverdue) {
        // Simple penalty calculation: 1% per month overdue
        BigDecimal monthsOverdue = new BigDecimal(daysOverdue)
                .divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        
        BigDecimal baseAmount = record.getAmount() != null ? 
                record.getAmount() : new BigDecimal("10000");
        
        return baseAmount.multiply(monthsOverdue)
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

