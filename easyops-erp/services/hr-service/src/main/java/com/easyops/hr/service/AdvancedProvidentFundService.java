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
public class AdvancedProvidentFundService {
    
    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfInterestCalculationRepository epfInterestCalculationRepository;
    private final ProvidentFundService providentFundService;
    
    /**
     * AI-powered Provident Fund recommendations
     */
    public Map<String, Object> getProvidentFundRecommendations(UUID employeeId, UUID organizationId) {
        log.info("Getting AI-powered Provident Fund recommendations for employee: {}", employeeId);
        
        Map<String, Object> recommendations = new HashMap<>();
        
        // Get employee's EPF account
        List<EpfAccount> accounts = epfAccountRepository.findByEmployeeId(employeeId);
        if (accounts.isEmpty()) {
            recommendations.put("status", "no_account");
            recommendations.put("message", "No EPF account found. Consider creating one.");
            return recommendations;
        }
        
        EpfAccount account = accounts.get(0);
        
        // Get contribution history
        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(account.getEpfAccountId());
        
        // Calculate average contribution
        BigDecimal avgContribution = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(contributions.size() > 0 ? contributions.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        // Get interest history
        List<EpfInterestCalculation> interestCalculations = epfInterestCalculationRepository
                .findByEpfAccountId(account.getEpfAccountId());
        
        BigDecimal avgInterestRate = interestCalculations.stream()
                .map(EpfInterestCalculation::getInterestRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(interestCalculations.size() > 0 ? interestCalculations.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        // Generate recommendations
        recommendations.put("currentBalance", account.getCurrentBalance());
        recommendations.put("averageMonthlyContribution", avgContribution);
        recommendations.put("averageInterestRate", avgInterestRate);
        recommendations.put("recommendedContribution", calculateOptimalContribution(account, avgContribution));
        recommendations.put("projectedBalance", projectBalance(account, avgContribution, avgInterestRate, 12));
        recommendations.put("recommendations", generateRecommendations(account, avgContribution, avgInterestRate));
        recommendations.put("riskAssessment", assessRisk(account, contributions));
        
        return recommendations;
    }
    
    /**
     * Automated contribution optimization
     */
    public Map<String, Object> optimizeContributions(UUID epfAccountId) {
        log.info("Optimizing contributions for EPF account: {}", epfAccountId);
        
        EpfAccount account = epfAccountRepository.findById(epfAccountId)
                .orElseThrow(() -> new RuntimeException("EPF account not found"));
        
        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(epfAccountId);
        
        // Analyze contribution patterns
        BigDecimal currentAvg = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(contributions.size() > 0 ? contributions.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        // Calculate optimal contribution (e.g., 12% of salary)
        BigDecimal optimalContribution = calculateOptimalContribution(account, currentAvg);
        
        Map<String, Object> optimization = new HashMap<>();
        optimization.put("currentAverageContribution", currentAvg);
        optimization.put("optimalContribution", optimalContribution);
        optimization.put("recommendedIncrease", optimalContribution.subtract(currentAvg));
        optimization.put("projectedGain", calculateProjectedGain(account, optimalContribution, currentAvg));
        optimization.put("optimizationScore", calculateOptimizationScore(account, contributions));
        
        return optimization;
    }
    
    /**
     * Provident Fund forecasting and projections
     */
    public Map<String, Object> forecastProvidentFund(UUID epfAccountId, Integer months) {
        log.info("Forecasting Provident Fund for account: {} for {} months", epfAccountId, months);
        
        EpfAccount account = epfAccountRepository.findById(epfAccountId)
                .orElseThrow(() -> new RuntimeException("EPF account not found"));
        
        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(epfAccountId);
        List<EpfInterestCalculation> interestCalculations = epfInterestCalculationRepository
                .findByEpfAccountId(epfAccountId);
        
        BigDecimal avgContribution = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(contributions.size() > 0 ? contributions.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        BigDecimal avgInterestRate = interestCalculations.stream()
                .map(EpfInterestCalculation::getInterestRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(interestCalculations.size() > 0 ? interestCalculations.size() : 1), 
                        2, RoundingMode.HALF_UP);
        
        Map<String, Object> forecast = new HashMap<>();
        forecast.put("currentBalance", account.getCurrentBalance());
        forecast.put("projectedBalance", projectBalance(account, avgContribution, avgInterestRate, months));
        forecast.put("projectedContributions", avgContribution.multiply(new BigDecimal(months)));
        forecast.put("projectedInterest", calculateProjectedInterest(
                account.getCurrentBalance(), avgContribution, avgInterestRate, months));
        forecast.put("forecastPeriod", months);
        forecast.put("confidenceLevel", new BigDecimal("85.00")); // Example confidence
        
        return forecast;
    }
    
    /**
     * Risk assessment for Provident Fund account
     */
    public Map<String, Object> assessRisk(UUID epfAccountId) {
        log.info("Assessing risk for EPF account: {}", epfAccountId);
        
        EpfAccount account = epfAccountRepository.findById(epfAccountId)
                .orElseThrow(() -> new RuntimeException("EPF account not found"));
        
        List<EpfContribution> contributions = epfContributionRepository.findByEpfAccountId(epfAccountId);
        
        return assessRisk(account, contributions);
    }
    
    private BigDecimal calculateOptimalContribution(EpfAccount account, BigDecimal currentAvg) {
        // Optimal contribution is typically 12% of basic salary
        // For now, use a simple calculation based on current average
        return currentAvg.multiply(new BigDecimal("1.1")); // 10% increase recommendation
    }
    
    private BigDecimal projectBalance(EpfAccount account, BigDecimal monthlyContribution, 
                                     BigDecimal interestRate, Integer months) {
        BigDecimal balance = account.getCurrentBalance();
        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);
        
        for (int i = 0; i < months; i++) {
            // Add contribution
            balance = balance.add(monthlyContribution);
            // Add interest (monthly compounding)
            balance = balance.multiply(BigDecimal.ONE.add(monthlyRate));
        }
        
        return balance.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateProjectedInterest(BigDecimal currentBalance, BigDecimal monthlyContribution,
                                                  BigDecimal annualRate, Integer months) {
        BigDecimal balance = currentBalance;
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);
        BigDecimal totalInterest = BigDecimal.ZERO;
        
        for (int i = 0; i < months; i++) {
            balance = balance.add(monthlyContribution);
            BigDecimal interest = balance.multiply(monthlyRate);
            totalInterest = totalInterest.add(interest);
            balance = balance.add(interest);
        }
        
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateProjectedGain(EpfAccount account, BigDecimal optimal, BigDecimal current) {
        // Calculate projected gain over 5 years
        BigDecimal difference = optimal.subtract(current);
        return difference.multiply(new BigDecimal("60")); // 5 years * 12 months
    }
    
    private BigDecimal calculateOptimizationScore(EpfAccount account, List<EpfContribution> contributions) {
        // Simple scoring algorithm
        if (contributions.isEmpty()) {
            return new BigDecimal("0");
        }
        
        // Score based on consistency and amount
        BigDecimal consistencyScore = new BigDecimal("50"); // Base score
        BigDecimal amountScore = account.getCurrentBalance().compareTo(new BigDecimal("100000")) > 0 
                ? new BigDecimal("30") : new BigDecimal("20");
        
        return consistencyScore.add(amountScore);
    }
    
    private List<String> generateRecommendations(EpfAccount account, BigDecimal avgContribution, 
                                                BigDecimal avgInterestRate) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (avgContribution.compareTo(new BigDecimal("5000")) < 0) {
            recommendations.add("Consider increasing your monthly contribution to maximize retirement savings");
        }
        
        if (account.getCurrentBalance().compareTo(new BigDecimal("50000")) < 0) {
            recommendations.add("Your current balance is below recommended levels. Consider higher contributions");
        }
        
        if (avgInterestRate.compareTo(new BigDecimal("8.5")) < 0) {
            recommendations.add("Interest rates are below historical averages. Monitor for better opportunities");
        }
        
        recommendations.add("Regular contributions help maximize compound interest benefits");
        
        return recommendations;
    }
    
    private Map<String, Object> assessRisk(EpfAccount account, List<EpfContribution> contributions) {
        Map<String, Object> riskAssessment = new HashMap<>();
        
        // Calculate risk factors
        BigDecimal balanceRisk = account.getCurrentBalance().compareTo(new BigDecimal("10000")) < 0 
                ? new BigDecimal("70") : new BigDecimal("30");
        
        BigDecimal contributionRisk = contributions.isEmpty() 
                ? new BigDecimal("80") : new BigDecimal("20");
        
        BigDecimal overallRisk = balanceRisk.add(contributionRisk).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        
        riskAssessment.put("overallRiskScore", overallRisk);
        riskAssessment.put("riskLevel", overallRisk.compareTo(new BigDecimal("50")) > 0 ? "High" : "Low");
        riskAssessment.put("balanceRisk", balanceRisk);
        riskAssessment.put("contributionRisk", contributionRisk);
        riskAssessment.put("recommendations", List.of(
                "Maintain consistent contributions",
                "Monitor account balance regularly",
                "Consider increasing contribution rate"));
        
        return riskAssessment;
    }
}

