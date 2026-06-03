package com.easyops.pharma.service;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import com.easyops.pharma.entity.IncentiveCalculation;
import com.easyops.pharma.entity.IncentiveDistribution;
import com.easyops.pharma.entity.Target;
import com.easyops.pharma.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncentiveService {
    
    private final IncentiveCalculationRepository calculationRepository;
    private final IncentiveDistributionRepository distributionRepository;
    private final TargetService targetService;
    private final DepositService depositService;
    private final ExpenseService expenseService;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    private final IncentiveRuleService incentiveRuleService;
    private final AccountingIntegrationService accountingIntegrationService;
    private final HrIntegrationService hrIntegrationService;
    
    
    @Transactional
    @CacheEvict(value = {"incentiveCalculations", "incentiveDistributions"}, allEntries = true)
    public IncentiveCalculation calculateIncentive(UUID territoryId, Integer year, Integer month) {
        return calculateIncentive(territoryId, year, month, false);
    }
    
    @Transactional
    @CacheEvict(value = {"incentiveCalculations", "incentiveDistributions"}, allEntries = true)
    public IncentiveCalculation calculateIncentive(UUID territoryId, Integer year, Integer month, boolean forceRecalculate) {
        log.info("Calculating incentive for territory: {}, year: {}, month: {}, forceRecalculate: {}", territoryId, year, month, forceRecalculate);
        
        // Check if calculation already exists
        Optional<IncentiveCalculation> existingOpt = calculationRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
        if (existingOpt.isPresent()) {
            if (!forceRecalculate) {
                log.warn("Incentive calculation already exists for territory: {}, year: {}, month: {}. Use forceRecalculate=true to refresh from deposits.", territoryId, year, month);
                return existingOpt.get();
            }
            IncentiveCalculation existing = existingOpt.get();
            if ("PAID".equals(existing.getStatus())) {
                throw new RuntimeException("Cannot recalculate incentives that have already been paid. Calculation ID: " + existing.getId());
            }
            log.info("Force recalculating - deleting existing calculation for territory: {}, year: {}, month: {}", territoryId, year, month);
            calculationRepository.delete(existing);
        }
        
        // Get target for this territory and month
        Optional<Target> targetOpt = targetService.getActiveTargetForTerritoryAndMonth(territoryId, year, month);
        if (targetOpt.isEmpty()) {
            throw new RuntimeException("No active target found for territory and month");
        }
        Target target = targetOpt.get();
        
        // Get covered amount from deposits
        BigDecimal coveredAmount = depositService.getTotalCoveredAmount(territoryId, year, month);
        
        // Get total expenses for the month
        BigDecimal totalExpenses = expenseService.getTotalExpensesForTerritoryAndMonth(territoryId, year, month);
        
        // Get territory-specific incentive rule (or defaults)
        LocalDate calculationDate = LocalDate.of(year, month, 1);
        IncentiveRuleService.TerritoryIncentiveRuleDTO rule = incentiveRuleService.getIncentiveRuleForTerritory(territoryId, calculationDate);
        
        // Calculate expense limit (using rule or default 30%)
        BigDecimal expenseLimitPercentage = rule.getExpenseLimitPercentage() != null ? rule.getExpenseLimitPercentage() : new BigDecimal("0.30");
        BigDecimal expenseLimit = target.getTargetAmount().multiply(expenseLimitPercentage);
        
        // Check eligibility
        boolean targetAchieved = coveredAmount.compareTo(target.getTargetAmount()) >= 0;
        boolean expenseWithinLimit = totalExpenses.compareTo(expenseLimit) <= 0;
        boolean territoryEligible = targetAchieved && expenseWithinLimit;
        
        // Create incentive calculation
        IncentiveCalculation calculation = new IncentiveCalculation();
        calculation.setOrganizationId(target.getOrganizationId());
        calculation.setTerritoryId(territoryId);
        calculation.setYear(year);
        calculation.setMonth(month);
        calculation.setTargetAmount(target.getTargetAmount());
        calculation.setCoveredAmount(coveredAmount);
        calculation.setTargetAchieved(targetAchieved);
        calculation.setExpenseWithinLimit(expenseWithinLimit);
        calculation.setTerritoryEligible(territoryEligible);
        calculation.setCalculationDate(LocalDateTime.now());
        
        // Calculate incentive base amount (using rule percentage or default 4%)
        BigDecimal incentivePercentage = rule.getIncentivePercentage() != null ? rule.getIncentivePercentage() : new BigDecimal("0.04");
        BigDecimal incentiveBaseAmount = coveredAmount.multiply(incentivePercentage);
        calculation.setIncentiveBaseAmount(incentiveBaseAmount);
        
        // If territory is eligible, calculate distribution (using rule percentages)
        if (territoryEligible) {
            List<IncentiveDistribution> distributions = calculateDistribution(territoryId, incentiveBaseAmount, year, month, rule);
            calculation.setDistributions(distributions);
            
            BigDecimal totalSrShare = distributions.stream()
                    .filter(d -> "SR_SHARE".equals(d.getDistributionType()))
                    .map(IncentiveDistribution::getIncentiveAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalMpoShare = distributions.stream()
                    .filter(d -> "MPO_SHARE".equals(d.getDistributionType()))
                    .map(IncentiveDistribution::getIncentiveAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalManagerShare = distributions.stream()
                    .filter(d -> "MANAGER_SHARE".equals(d.getDistributionType()))
                    .map(IncentiveDistribution::getIncentiveAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDevelopmentFund = distributions.stream()
                    .filter(d -> "DEVELOPMENT_FUND".equals(d.getDistributionType()))
                    .map(IncentiveDistribution::getIncentiveAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            calculation.setTotalSrShare(totalSrShare);
            calculation.setTotalMpoShare(totalMpoShare);
            calculation.setTotalManagerShare(totalManagerShare);
            calculation.setTotalIncentiveDistributed(
                    totalSrShare.add(totalMpoShare).add(totalManagerShare).add(totalDevelopmentFund));
        } else {
            calculation.setTotalSrShare(BigDecimal.ZERO);
            calculation.setTotalMpoShare(BigDecimal.ZERO);
            calculation.setTotalManagerShare(BigDecimal.ZERO);
            calculation.setTotalIncentiveDistributed(BigDecimal.ZERO);
        }
        
        // Save calculation
        IncentiveCalculation savedCalculation = calculationRepository.save(calculation);
        
        // Save distributions
        if (savedCalculation.getDistributions() != null) {
            for (IncentiveDistribution distribution : savedCalculation.getDistributions()) {
                distribution.setIncentiveCalculationId(savedCalculation.getId());
                distribution.setIncentiveCalculation(savedCalculation); // Set object reference for integration hooks
                distribution.setCalculationDate(LocalDateTime.now());
                distributionRepository.save(distribution);
            }
        }
        
        // Phase 5.3: Integration hooks
        // Post journal entry to Accounting Service
        try {
            accountingIntegrationService.postIncentiveJournalEntry(savedCalculation);
        } catch (Exception e) {
            log.warn("Failed to post incentive journal entry: {}", e.getMessage());
        }
        
        // Create incentive bonuses in HR Service for payroll processing (exclude DEVELOPMENT_FUND - no employee)
        if (savedCalculation.getDistributions() != null && !savedCalculation.getDistributions().isEmpty()) {
            try {
                List<IncentiveDistribution> employeeDistributions = savedCalculation.getDistributions().stream()
                        .filter(d -> d.getEmployeeId() != null)
                        .collect(Collectors.toList());
                hrIntegrationService.createIncentiveBonuses(employeeDistributions, year, month);
            } catch (Exception e) {
                log.warn("Failed to create incentive bonuses in HR service: {}", e.getMessage());
            }
        }
        
        return savedCalculation;
    }
    
    private List<IncentiveDistribution> calculateDistribution(UUID territoryId, BigDecimal incentiveBaseAmount, Integer year, Integer month, IncentiveRuleService.TerritoryIncentiveRuleDTO rule) {
        List<IncentiveDistribution> distributions = new ArrayList<>();
        LocalDate calculationRefDate = LocalDate.of(year, month, 1);

        // 1. SR share: incentiveBaseAmount × srSharePercentage
        BigDecimal srSharePercentage = rule.getSrSharePercentage() != null ? rule.getSrSharePercentage() : new BigDecimal("0.09");
        BigDecimal srShare = incentiveBaseAmount.multiply(srSharePercentage).setScale(2, RoundingMode.HALF_UP);

        if (Boolean.TRUE.equals(rule.getHasDedicatedSr())) {
            // Dedicated SR: find SR in assignments
            List<EmployeeTerritoryAssignment> srAssignments = assignmentRepository.findActiveAssignmentsByTerritory(territoryId, calculationRefDate).stream()
                    .filter(a -> "SR".equals(a.getRoleInTerritory()))
                    .collect(Collectors.toList());
            if (!srAssignments.isEmpty()) {
                IncentiveDistribution srDist = createDistribution(srAssignments.get(0).getEmployeeId(), territoryId, srAssignments.get(0).getRoleInTerritory(), srShare, "SR_SHARE");
                distributions.add(srDist);
            }
        } else {
            // Dual-role: SR share goes to dualRoleEmployeeId
            UUID dualRoleId = rule.getDualRoleEmployeeId();
            if (dualRoleId != null) {
                List<EmployeeTerritoryAssignment> assignments = assignmentRepository.findActiveAssignmentsByTerritory(territoryId, calculationRefDate);
                String role = assignments.stream().filter(a -> dualRoleId.equals(a.getEmployeeId())).findFirst().map(EmployeeTerritoryAssignment::getRoleInTerritory).orElse("MPO");
                IncentiveDistribution srDist = createDistribution(dualRoleId, territoryId, role, srShare, "SR_SHARE");
                distributions.add(srDist);
            }
        }

        // 2. Development fund: incentiveBaseAmount × developmentFundPercentage
        BigDecimal devFundPercentage = rule.getDevelopmentFundPercentage() != null ? rule.getDevelopmentFundPercentage() : new BigDecimal("0.01");
        BigDecimal developmentFund = incentiveBaseAmount.multiply(devFundPercentage).setScale(2, RoundingMode.HALF_UP);
        IncentiveDistribution devFundDist = createDistribution(null, territoryId, "DEVELOPMENT_FUND", developmentFund, "DEVELOPMENT_FUND");
        distributions.add(devFundDist);

        // 3. Remaining pool = incentiveBaseAmount - SR share - Development fund
        BigDecimal remainingPool = incentiveBaseAmount.subtract(srShare).subtract(developmentFund);

        // 4. Manager/MPO: use allocations; remainingPool × (allocationPercentage / 100) per employee
        List<IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO> allocations = rule.getAllocations();
        if (allocations != null && !allocations.isEmpty()) {
            for (IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO alloc : allocations) {
                BigDecimal allocationPct = alloc.allocationPercentage() != null ? alloc.allocationPercentage() : BigDecimal.ZERO;
                BigDecimal amount = remainingPool.multiply(allocationPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                String distType = isManagerRole(alloc.roleInTerritory()) ? "MANAGER_SHARE" : "MPO_SHARE";
                IncentiveDistribution dist = createDistribution(alloc.employeeId(), territoryId, alloc.roleInTerritory(), amount, distType);
                distributions.add(dist);
            }
        }

        return distributions;
    }

    private IncentiveDistribution createDistribution(UUID employeeId, UUID territoryId, String roleInArea, BigDecimal amount, String distributionType) {
        IncentiveDistribution d = new IncentiveDistribution();
        d.setEmployeeId(employeeId);
        d.setTerritoryId(territoryId);
        d.setRoleInArea(roleInArea);
        d.setIncentiveAmount(amount);
        d.setDistributionType(distributionType);
        d.setStatus("CALCULATED");
        return d;
    }

    private boolean isManagerRole(String role) {
        if (role == null) return false;
        return role.contains("AM") || role.contains("TM") || role.contains("RM") || role.contains("DSM") || role.contains("ASM") || role.contains("SM");
    }
    
    @Transactional(readOnly = true)
    public List<IncentiveCalculation> getCalculationsByTerritory(UUID territoryId, Integer year) {
        log.debug("Fetching incentive calculations for territory: {}, year: {}", territoryId, year);
        return calculationRepository.findByTerritoryAndYear(territoryId, year);
    }
    
    @Transactional(readOnly = true)
    public Optional<IncentiveCalculation> getCalculationByTerritoryAndMonth(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching incentive calculation for territory: {}, year: {}, month: {}", territoryId, year, month);
        return calculationRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }
    
    @Transactional(readOnly = true)
    public List<IncentiveDistribution> getDistributionsByEmployee(UUID employeeId) {
        log.debug("Fetching incentive distributions for employee: {}", employeeId);
        return distributionRepository.findByEmployeeId(employeeId);
    }
    
    @Transactional(readOnly = true)
    public IncentiveCalculation getCalculationById(UUID calculationId) {
        return calculationRepository.findById(calculationId)
                .orElseThrow(() -> new RuntimeException("Incentive calculation not found"));
    }

    /**
     * Organization for RBAC when listing employee distributions; null if none.
     */
    @Transactional(readOnly = true)
    public UUID resolveOrganizationIdForEmployeeDistributions(UUID employeeId) {
        List<IncentiveDistribution> list = distributionRepository.findByEmployeeId(employeeId);
        if (list.isEmpty()) {
            return null;
        }
        return calculationRepository.findById(list.get(0).getIncentiveCalculationId())
                .map(IncentiveCalculation::getOrganizationId)
                .orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"incentiveCalculations", "incentiveDistributions"}, allEntries = true)
    public void markIncentivesAsPaid(UUID calculationId) {
        log.info("Marking incentives as paid for calculation: {}", calculationId);
        IncentiveCalculation calculation = calculationRepository.findById(calculationId)
                .orElseThrow(() -> new RuntimeException("Incentive calculation not found"));
        
        calculation.setStatus("PAID");
        calculationRepository.save(calculation);
        
        List<IncentiveDistribution> distributions = distributionRepository.findByIncentiveCalculationId(calculationId);
        for (IncentiveDistribution distribution : distributions) {
            distribution.setStatus("PAID");
            distribution.setPaidDate(LocalDateTime.now());
            distributionRepository.save(distribution);
        }
    }
}

