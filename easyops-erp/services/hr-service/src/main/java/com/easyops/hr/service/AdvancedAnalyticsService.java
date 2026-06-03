package com.easyops.hr.service;

import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Transactional
public class AdvancedAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedAnalyticsService.class);
    
    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EmployeeRepository employeeRepository;
    
    /**
     * Provident Fund participation forecasting
     */
    public Map<String, Object> forecastProvidentFundParticipation(UUID organizationId, Integer months) {
        log.info("Forecasting Provident Fund participation for organization: {} for {} months", 
                organizationId, months);
        
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        List<Employee> employees = employeeRepository.findByOrganizationId(organizationId);
        
        int currentEnrollment = accounts.size();
        int totalEmployees = employees.size();
        BigDecimal currentParticipationRate = new BigDecimal(currentEnrollment)
                .divide(new BigDecimal(totalEmployees > 0 ? totalEmployees : 1), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        // Get historical enrollment trends
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId);
        
        // Simple growth projection (can be enhanced with ML)
        BigDecimal growthRate = new BigDecimal("2.00"); // 2% monthly growth assumption
        BigDecimal projectedEnrollment = new BigDecimal(currentEnrollment)
                .multiply(BigDecimal.ONE.add(growthRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .pow(months)
                .setScale(0, RoundingMode.HALF_UP);
        
        BigDecimal projectedParticipationRate = projectedEnrollment
                .divide(new BigDecimal(totalEmployees > 0 ? totalEmployees : 1), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        Map<String, Object> forecast = new HashMap<>();
        forecast.put("organizationId", organizationId);
        forecast.put("forecastPeriod", months + " months");
        forecast.put("currentEnrollment", currentEnrollment);
        forecast.put("currentParticipationRate", currentParticipationRate.setScale(2, RoundingMode.HALF_UP));
        forecast.put("projectedEnrollment", projectedEnrollment);
        forecast.put("projectedParticipationRate", projectedParticipationRate.setScale(2, RoundingMode.HALF_UP));
        forecast.put("growthRate", growthRate);
        forecast.put("confidenceLevel", new BigDecimal("70.00")); // Example confidence
        
        return forecast;
    }
    
    /**
     * Trend analysis and pattern recognition
     */
    public Map<String, Object> analyzeTrendsAndPatterns(UUID organizationId, String entityType, Integer months) {
        log.info("Analyzing trends and patterns for organization: {}, type: {}, months: {}", 
                organizationId, entityType, months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        Map<String, Object> analysis = new HashMap<>();
        
        if ("provident_fund".equals(entityType)) {
            List<EpfContribution> contributions = epfContributionRepository
                    .findByOrganizationId(organizationId).stream()
                    .filter(c -> {
                        LocalDate contribDate = LocalDate.of(c.getContributionYear(), c.getContributionMonth(), 1);
                        return !contribDate.isBefore(startDate) && !contribDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());
            
            // Monthly trend
            Map<String, BigDecimal> monthlyTrend = contributions.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getContributionMonth() + "/" + c.getContributionYear(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    EpfContribution::getTotalContribution,
                                    BigDecimal::add)));
            
            analysis.put("type", "provident_fund");
            analysis.put("monthlyTrend", monthlyTrend);
            analysis.put("pattern", detectPattern(monthlyTrend));
        } else {
            analysis.put("type", entityType);
            analysis.put("message", "Unsupported entity type. Use provident_fund.");
        }
        
        analysis.put("organizationId", organizationId);
        analysis.put("period", months + " months");
        
        return analysis;
    }
    
    private String detectPattern(Map<String, BigDecimal> monthlyTrend) {
        if (monthlyTrend.size() < 3) {
            return "insufficient_data";
        }
        
        List<BigDecimal> amounts = monthlyTrend.values().stream()
                .sorted()
                .collect(Collectors.toList());
        
        BigDecimal first = amounts.get(0);
        BigDecimal last = amounts.get(amounts.size() - 1);
        BigDecimal change = last.subtract(first);
        
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "increasing";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return "decreasing";
        } else {
            return "stable";
        }
    }
}

