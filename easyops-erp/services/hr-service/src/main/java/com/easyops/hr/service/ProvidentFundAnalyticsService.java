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
public class ProvidentFundAnalyticsService {
    
    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfInterestCalculationRepository epfInterestCalculationRepository;
    private final EmployeeRepository employeeRepository;
    
    /**
     * Get participation metrics and dashboards
     */
    public Map<String, Object> getParticipationMetrics(UUID organizationId) {
        log.info("Getting participation metrics for organization: {}", organizationId);
        
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        List<Employee> employees = employeeRepository.findByOrganizationId(organizationId);
        
        int totalEmployees = employees.size();
        int enrolledEmployees = accounts.size();
        BigDecimal participationRate = new BigDecimal(enrolledEmployees)
                .divide(new BigDecimal(totalEmployees > 0 ? totalEmployees : 1), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalEmployees", totalEmployees);
        metrics.put("enrolledEmployees", enrolledEmployees);
        metrics.put("participationRate", participationRate);
        metrics.put("activeAccounts", accounts.stream().filter(a -> a.getIsActive()).count());
        metrics.put("inactiveAccounts", accounts.stream().filter(a -> !a.getIsActive()).count());
        
        return metrics;
    }
    
    /**
     * Contribution analysis and trends
     */
    public Map<String, Object> analyzeContributions(UUID organizationId, LocalDate startDate, LocalDate endDate) {
        log.info("Analyzing contributions for organization: {} from {} to {}", 
                organizationId, startDate, endDate);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(c -> {
                    LocalDate contributionDate = LocalDate.of(
                            c.getContributionYear(), c.getContributionMonth(), 1);
                    return !contributionDate.isBefore(startDate) && !contributionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        
        BigDecimal totalEmployeeContributions = contributions.stream()
                .map(EpfContribution::getEmployeeContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEmployerContributions = contributions.stream()
                .map(EpfContribution::getEmployerContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalContributions = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgMonthlyContribution = totalContributions
                .divide(new BigDecimal(contributions.size() > 0 ? contributions.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("period", startDate + " to " + endDate);
        analysis.put("totalContributions", contributions.size());
        analysis.put("totalEmployeeContributions", totalEmployeeContributions);
        analysis.put("totalEmployerContributions", totalEmployerContributions);
        analysis.put("totalContributions", totalContributions);
        analysis.put("averageMonthlyContribution", avgMonthlyContribution);
        analysis.put("trend", calculateContributionTrend(contributions));
        
        return analysis;
    }
    
    /**
     * Cost analysis and optimization
     */
    public Map<String, Object> analyzeCosts(UUID organizationId, Integer year) {
        log.info("Analyzing Provident Fund costs for organization: {}, year: {}", organizationId, year);
        
        List<EpfContribution> contributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(c -> c.getContributionYear().equals(year))
                .collect(Collectors.toList());
        
        BigDecimal totalEmployerCost = contributions.stream()
                .map(EpfContribution::getEmployerContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEmployeeCost = contributions.stream()
                .map(EpfContribution::getEmployeeContributionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = totalEmployerCost.add(totalEmployeeCost);
        
        // Calculate per employee cost
        long uniqueEmployees = contributions.stream()
                .map(EpfContribution::getEmployeeId)
                .distinct()
                .count();
        
        BigDecimal avgCostPerEmployee = totalCost
                .divide(new BigDecimal(uniqueEmployees > 0 ? uniqueEmployees : 1), 
                        2, RoundingMode.HALF_UP);
        
        Map<String, Object> costAnalysis = new HashMap<>();
        costAnalysis.put("year", year);
        costAnalysis.put("totalEmployerCost", totalEmployerCost);
        costAnalysis.put("totalEmployeeCost", totalEmployeeCost);
        costAnalysis.put("totalCost", totalCost);
        costAnalysis.put("averageCostPerEmployee", avgCostPerEmployee);
        costAnalysis.put("totalEmployees", uniqueEmployees);
        costAnalysis.put("optimizationSuggestions", generateCostOptimizationSuggestions(totalCost, avgCostPerEmployee));
        
        return costAnalysis;
    }
    
    /**
     * ROI measurement
     */
    public Map<String, Object> measureROI(UUID organizationId, Integer year) {
        log.info("Measuring ROI for organization: {}, year: {}", organizationId, year);
        
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        // financialYear is Integer - compare directly
        List<EpfInterestCalculation> interestCalculations = epfInterestCalculationRepository
                .findByOrganizationId(organizationId).stream()
                .filter(ic -> ic.getFinancialYear() != null && ic.getFinancialYear().equals(year))
                .collect(Collectors.toList());
        
        BigDecimal totalInterestEarned = interestCalculations.stream()
                .map(EpfInterestCalculation::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalContributions = epfContributionRepository
                .findByOrganizationId(organizationId).stream()
                .filter(c -> c.getContributionYear().equals(year))
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal roi = totalContributions.compareTo(BigDecimal.ZERO) > 0
                ? totalInterestEarned.divide(totalContributions, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        Map<String, Object> roiAnalysis = new HashMap<>();
        roiAnalysis.put("year", year);
        roiAnalysis.put("totalContributions", totalContributions);
        roiAnalysis.put("totalInterestEarned", totalInterestEarned);
        roiAnalysis.put("roiPercentage", roi.setScale(2, RoundingMode.HALF_UP));
        roiAnalysis.put("totalAccounts", accounts.size());
        roiAnalysis.put("averageROIPerAccount", roi);
        
        return roiAnalysis;
    }
    
    /**
     * Impact analysis
     */
    public Map<String, Object> analyzeImpact(UUID organizationId) {
        log.info("Analyzing Provident Fund impact for organization: {}", organizationId);
        
        List<EpfAccount> accounts = epfAccountRepository.findByOrganizationId(organizationId);
        BigDecimal totalBalance = accounts.stream()
                .map(EpfAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgBalance = totalBalance
                .divide(new BigDecimal(accounts.size() > 0 ? accounts.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        List<EpfContribution> allContributions = epfContributionRepository
                .findByOrganizationId(organizationId);
        
        BigDecimal totalContributionsEver = allContributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> impact = new HashMap<>();
        impact.put("totalAccounts", accounts.size());
        impact.put("totalBalance", totalBalance);
        impact.put("averageBalance", avgBalance);
        impact.put("totalContributionsEver", totalContributionsEver);
        impact.put("totalInterestEarned", totalBalance.subtract(totalContributionsEver));
        impact.put("impactScore", calculateImpactScore(accounts.size(), totalBalance, avgBalance));
        
        return impact;
    }
    
    private Map<String, Object> calculateContributionTrend(List<EpfContribution> contributions) {
        Map<String, Object> trend = new HashMap<>();
        
        if (contributions.size() < 2) {
            trend.put("direction", "insufficient_data");
            return trend;
        }
        
        // Group by month and calculate monthly totals
        Map<String, BigDecimal> monthlyTotals = contributions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getContributionMonth() + "/" + c.getContributionYear(),
                        Collectors.reducing(BigDecimal.ZERO,
                                EpfContribution::getTotalContribution,
                                BigDecimal::add)));
        
        List<BigDecimal> amounts = monthlyTotals.values().stream()
                .sorted()
                .collect(Collectors.toList());
        
        if (amounts.size() >= 2) {
            BigDecimal first = amounts.get(0);
            BigDecimal last = amounts.get(amounts.size() - 1);
            BigDecimal change = last.subtract(first);
            BigDecimal changePercent = first.compareTo(BigDecimal.ZERO) > 0
                    ? change.divide(first, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            trend.put("direction", change.compareTo(BigDecimal.ZERO) > 0 ? "increasing" : "decreasing");
            trend.put("changePercentage", changePercent.setScale(2, RoundingMode.HALF_UP));
            trend.put("firstMonthTotal", first);
            trend.put("lastMonthTotal", last);
        }
        
        return trend;
    }
    
    private List<String> generateCostOptimizationSuggestions(BigDecimal totalCost, BigDecimal avgCostPerEmployee) {
        List<String> suggestions = new java.util.ArrayList<>();
        
        if (avgCostPerEmployee.compareTo(new BigDecimal("5000")) > 0) {
            suggestions.add("Average cost per employee is high. Consider reviewing contribution rates");
        }
        
        if (totalCost.compareTo(new BigDecimal("1000000")) > 0) {
            suggestions.add("Total cost exceeds 1M. Consider bulk contribution optimization");
        }
        
        suggestions.add("Regular monitoring helps identify cost-saving opportunities");
        suggestions.add("Consider automated contribution processing to reduce administrative costs");
        
        return suggestions;
    }
    
    private BigDecimal calculateImpactScore(int accountCount, BigDecimal totalBalance, BigDecimal avgBalance) {
        // Simple impact scoring
        BigDecimal accountScore = new BigDecimal(Math.min(accountCount * 2, 50));
        BigDecimal balanceScore = totalBalance.compareTo(new BigDecimal("1000000")) > 0 
                ? new BigDecimal("30") : new BigDecimal("20");
        BigDecimal avgScore = avgBalance.compareTo(new BigDecimal("50000")) > 0 
                ? new BigDecimal("20") : new BigDecimal("10");
        
        return accountScore.add(balanceScore).add(avgScore);
    }
}

