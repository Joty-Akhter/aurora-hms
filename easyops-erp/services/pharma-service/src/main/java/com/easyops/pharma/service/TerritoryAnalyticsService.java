package com.easyops.pharma.service;

import com.easyops.pharma.entity.*;
import com.easyops.pharma.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryAnalyticsService {
    
    private final TerritoryRepository territoryRepository;
    private final AreaRepository areaRepository;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    private final DepositRepository depositRepository;
    private final DepositLineRepository depositLineRepository;
    private final TargetRepository targetRepository;
    private final TargetCoverageRepository targetCoverageRepository;
    private final ExpenseRepository expenseRepository;
    private final IncentiveCalculationRepository incentiveCalculationRepository;
    private final ProductDisbursementRepository disbursementRepository;
    
    /**
     * Get territory performance analytics - uses territory-level data directly
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTerritoryPerformanceAnalytics(
            UUID territoryId, Integer year, Integer month) {
        log.info("Generating territory performance analytics for territory: {}, year: {}, month: {}", 
                territoryId, year, month);
        
        Territory territory = territoryRepository.findById(territoryId)
                .orElseThrow(() -> new RuntimeException("Territory not found: " + territoryId));
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("territoryId", territoryId);
        analytics.put("territoryName", territory.getName());
        analytics.put("year", year);
        analytics.put("month", month);
        
        Map<String, Object> territoryMetrics = getTerritoryMetrics(territoryId, year, month);
        
        BigDecimal targetAmount = (BigDecimal) territoryMetrics.getOrDefault("targetAmount", BigDecimal.ZERO);
        BigDecimal coveredAmount = (BigDecimal) territoryMetrics.getOrDefault("coveredAmount", BigDecimal.ZERO);
        BigDecimal totalExpenses = (BigDecimal) territoryMetrics.getOrDefault("totalExpenses", BigDecimal.ZERO);
        BigDecimal incentiveAmount = (BigDecimal) territoryMetrics.getOrDefault("incentiveAmount", BigDecimal.ZERO);
        BigDecimal totalDeposits = (BigDecimal) territoryMetrics.getOrDefault("totalDeposits", BigDecimal.ZERO);
        int employeeCount = (Integer) territoryMetrics.getOrDefault("employeeCount", 0);
        boolean targetAchieved = (Boolean) territoryMetrics.getOrDefault("targetAchieved", false);
        boolean incentiveEligible = (Boolean) territoryMetrics.getOrDefault("incentiveEligible", false);
        
        BigDecimal targetAchievementRate = targetAmount.compareTo(BigDecimal.ZERO) > 0
                ? coveredAmount.divide(targetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        BigDecimal expenseRatio = targetAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalExpenses.divide(targetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        BigDecimal incentiveRate = coveredAmount.compareTo(BigDecimal.ZERO) > 0
                ? incentiveAmount.divide(coveredAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        BigDecimal territoryEfficiencyScore = calculateTerritoryEfficiencyScore(
                targetAchievementRate, expenseRatio, targetAchieved);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEmployees", employeeCount);
        summary.put("totalTarget", targetAmount);
        summary.put("totalCovered", coveredAmount);
        summary.put("totalExpenses", totalExpenses);
        summary.put("totalIncentives", incentiveAmount);
        summary.put("totalDeposits", totalDeposits);
        summary.put("targetAchievementRate", targetAchievementRate);
        summary.put("expenseRatio", expenseRatio);
        summary.put("incentiveRate", incentiveRate);
        summary.put("territoryEfficiencyScore", territoryEfficiencyScore);
        summary.put("targetAchieved", targetAchieved);
        summary.put("incentiveEligible", incentiveEligible);
        
        analytics.put("summary", summary);
        analytics.put("territoryMetrics", territoryMetrics);
        
        if (month > 1) {
            Map<String, Object> previousMonthAnalytics = getTerritoryPerformanceAnalytics(
                    territoryId, year, month - 1);
            analytics.put("trend", calculateTrend(analytics, previousMonthAnalytics));
        }
        
        return analytics;
    }
    
    private Map<String, Object> getTerritoryMetrics(UUID territoryId, Integer year, Integer month) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("territoryId", territoryId);
        
        Optional<Territory> territoryOpt = territoryRepository.findById(territoryId);
        if (territoryOpt.isPresent()) {
            metrics.put("territoryName", territoryOpt.get().getName());
        }
        
        Optional<Target> targetOpt = targetRepository.findActiveTargetForTerritoryAndMonth(territoryId, year, month);
        BigDecimal targetAmount = targetOpt.map(Target::getTargetAmount).orElse(BigDecimal.ZERO);
        metrics.put("targetAmount", targetAmount);
        metrics.put("hasTarget", targetOpt.isPresent());
        
        List<Deposit> deposits = depositRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
        BigDecimal coveredAmount = deposits.stream()
                .filter(d -> "COMPLETED".equals(d.getStatus()))
                .flatMap(d -> depositLineRepository.findByDepositId(d.getId()).stream())
                .map(dl -> dl.getProductAmount() != null ? dl.getProductAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.put("coveredAmount", coveredAmount);
        
        BigDecimal totalDeposits = deposits.stream()
                .map(d -> d.getDepositAmount() != null ? d.getDepositAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.put("totalDeposits", totalDeposits);
        metrics.put("depositCount", deposits.size());
        
        BigDecimal totalExpenses = expenseRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month)
                .stream()
                .map(e -> e.getExpenseAmount() != null ? e.getExpenseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.put("totalExpenses", totalExpenses);
        
        Optional<IncentiveCalculation> incentiveOpt = incentiveCalculationRepository
                .findByTerritoryIdAndYearAndMonth(territoryId, year, month);
        BigDecimal incentiveAmount = incentiveOpt.map(IncentiveCalculation::getIncentiveBaseAmount)
                .orElse(BigDecimal.ZERO);
        metrics.put("incentiveAmount", incentiveAmount);
        metrics.put("incentiveEligible", incentiveOpt.map(IncentiveCalculation::getTerritoryEligible).orElse(false));
        
        boolean targetAchieved = targetAmount.compareTo(BigDecimal.ZERO) > 0
                && coveredAmount.compareTo(targetAmount) >= 0;
        metrics.put("targetAchieved", targetAchieved);
        
        BigDecimal achievementRate = targetAmount.compareTo(BigDecimal.ZERO) > 0
                ? coveredAmount.divide(targetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        metrics.put("achievementRate", achievementRate);
        
        int employeeCount = assignmentRepository.findByTerritoryId(territoryId).size();
        metrics.put("employeeCount", employeeCount);
        
        return metrics;
    }
    
    private BigDecimal calculateTerritoryEfficiencyScore(
            BigDecimal targetAchievementRate, BigDecimal expenseRatio, boolean targetAchieved) {
        BigDecimal achievementScore = targetAchievementRate.multiply(BigDecimal.valueOf(0.5));
        
        BigDecimal expenseScore = BigDecimal.ZERO;
        if (expenseRatio.compareTo(BigDecimal.valueOf(30)) <= 0) {
            expenseScore = BigDecimal.valueOf(30).multiply(BigDecimal.valueOf(0.3));
        } else if (expenseRatio.compareTo(BigDecimal.valueOf(100)) < 0) {
            expenseScore = BigDecimal.valueOf(30)
                    .subtract(expenseRatio.subtract(BigDecimal.valueOf(30)))
                    .divide(BigDecimal.valueOf(70), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(30));
        }
        
        BigDecimal targetScore = targetAchieved ? BigDecimal.valueOf(20) : BigDecimal.ZERO;
        
        return achievementScore.add(expenseScore).add(targetScore)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private Map<String, Object> calculateTrend(
            Map<String, Object> current, Map<String, Object> previous) {
        Map<String, Object> trend = new HashMap<>();
        
        Map<String, Object> currentSummary = (Map<String, Object>) current.get("summary");
        Map<String, Object> previousSummary = (Map<String, Object>) previous.get("summary");
        
        if (currentSummary != null && previousSummary != null) {
            BigDecimal currentCovered = (BigDecimal) currentSummary.get("totalCovered");
            BigDecimal previousCovered = (BigDecimal) previousSummary.get("totalCovered");
            
            if (previousCovered != null && previousCovered.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changePercent = currentCovered.subtract(previousCovered)
                        .divide(previousCovered, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                trend.put("coverageChangePercent", changePercent);
                trend.put("trendDirection", changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "UP" : "DOWN");
            }
        }
        
        return trend;
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getTerritoryOptimizationRecommendations(UUID territoryId) {
        log.info("Generating territory optimization recommendations for territory: {}", territoryId);
        
        Territory territory = territoryRepository.findById(territoryId)
                .orElseThrow(() -> new RuntimeException("Territory not found: " + territoryId));
        
        LocalDate now = LocalDate.now();
        Integer year = now.getYear();
        Integer month = now.getMonthValue();
        
        Map<String, Object> recommendations = new HashMap<>();
        recommendations.put("territoryId", territoryId);
        recommendations.put("territoryName", territory.getName());
        
        Map<String, Object> territoryMetrics = getTerritoryMetrics(territoryId, year, month);
        int employeeCount = (Integer) territoryMetrics.getOrDefault("employeeCount", 0);
        Optional<Target> targetOpt = targetRepository.findActiveTargetForTerritoryAndMonth(territoryId, year, month);
        
        List<Map<String, Object>> workloadAnalysis = new ArrayList<>();
        List<Map<String, Object>> performanceGaps = new ArrayList<>();
        
        if (targetOpt.isPresent()) {
            BigDecimal targetAmount = targetOpt.get().getTargetAmount();
            BigDecimal workloadPerEmployee = employeeCount > 0
                    ? targetAmount.divide(BigDecimal.valueOf(employeeCount), 2, RoundingMode.HALF_UP)
                    : targetAmount;
            
            workloadAnalysis.add(Map.of(
                    "territoryId", territoryId,
                    "territoryName", territory.getName(),
                    "employeeCount", employeeCount,
                    "targetAmount", targetAmount,
                    "workloadPerEmployee", workloadPerEmployee
            ));
        }
        
        BigDecimal achievementRate = (BigDecimal) territoryMetrics.get("achievementRate");
        if (achievementRate != null && achievementRate.compareTo(BigDecimal.valueOf(80)) < 0) {
            performanceGaps.add(Map.of(
                    "territoryId", territoryId,
                    "territoryName", territory.getName(),
                    "achievementRate", achievementRate,
                    "recommendation", "Review employee assignments and provide additional support"
            ));
        }
        
        recommendations.put("workloadAnalysis", workloadAnalysis);
        recommendations.put("performanceGaps", performanceGaps);
        
        return recommendations;
    }
}
