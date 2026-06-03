package com.easyops.pharma.service;

import com.easyops.pharma.entity.*;
import com.easyops.pharma.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    
    private final TargetRepository targetRepository;
    private final TargetCoverageRepository coverageRepository;
    private final DepositRepository depositRepository;
    private final TerritoryRepository territoryRepository;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    @Qualifier("requiresNewTransactionTemplate")
    private final TransactionTemplate requiresNewTransactionTemplate;
    
    @Transactional(readOnly = true)
    public List<Target> getAllTargets(UUID organizationId) {
        log.debug("Fetching all targets for organization: {}", organizationId);
        return targetRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Target> getTargetsByTerritory(UUID territoryId) {
        log.debug("Fetching targets for territory: {}", territoryId);
        return targetRepository.findByTerritoryId(territoryId);
    }
    
    @Transactional(readOnly = true)
    public List<Target> getTargetsByEmployee(UUID employeeId) {
        log.debug("Fetching targets for employee: {}", employeeId);
        return targetRepository.findByEmployeeId(employeeId);
    }
    
    @Transactional(readOnly = true)
    public Target getTargetById(UUID id) {
        log.debug("Fetching target by ID: {}", id);
        return targetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target not found with ID: " + id));
    }
    
    @Transactional(readOnly = true)
    public Optional<Target> getActiveTargetForTerritoryAndMonth(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching active target for territory: {}, year: {}, month: {}", territoryId, year, month);
        return targetRepository.findActiveTargetForTerritoryAndMonth(territoryId, year, month);
    }
    
    @Transactional
    @CacheEvict(value = {"targets", "targetCoverage"}, allEntries = true)
    public Target createTarget(Target target) {
        log.info("Creating new target for territory: {}", target.getTerritoryId());
        
        // Validate territory exists
        Territory territory = territoryRepository.findById(target.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + target.getTerritoryId()));
        
        // Validate employee belongs to territory
        boolean employeeBelongsToTerritory = assignmentRepository.findByTerritoryId(target.getTerritoryId()).stream()
                .anyMatch(a -> a.getEmployeeId().equals(target.getEmployeeId()));
        
        if (!employeeBelongsToTerritory) {
            throw new RuntimeException("Employee does not belong to selected territory");
        }
        
        // Validate month range
        if (target.getStartMonth() > target.getEndMonth()) {
            throw new RuntimeException("Start month cannot be greater than end month");
        }
        
        if (target.getStartMonth() < 1 || target.getStartMonth() > 12 || 
            target.getEndMonth() < 1 || target.getEndMonth() > 12) {
            throw new RuntimeException("Month must be between 1 and 12");
        }
        
        if (target.getTargetAmount() == null || target.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Target amount must be greater than zero");
        }
        
        return targetRepository.save(target);
    }
    
    @Transactional
    @CacheEvict(value = {"targets", "targetCoverage"}, allEntries = true)
    public Target updateTarget(UUID id, Target target) {
        log.info("Updating target: {}", id);
        Target existing = getTargetById(id);
        
        existing.setTerritoryId(target.getTerritoryId());
        existing.setEmployeeId(target.getEmployeeId());
        existing.setYear(target.getYear());
        existing.setStartMonth(target.getStartMonth());
        existing.setEndMonth(target.getEndMonth());
        existing.setTargetAmount(target.getTargetAmount());
        existing.setStatus(target.getStatus());
        existing.setUpdatedBy(target.getUpdatedBy());
        
        return targetRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"targets", "targetCoverage"}, allEntries = true)
    public void deleteTarget(UUID id) {
        log.info("Deleting target: {}", id);
        Target target = getTargetById(id);
        targetRepository.delete(target);
    }
    
    @Transactional
    @CacheEvict(value = "targetCoverage", allEntries = true)
    public TargetCoverage calculateCoverage(UUID territoryId, Integer year, Integer month) {
        log.info("Calculating target coverage for territory: {}, year: {}, month: {}", territoryId, year, month);
        
        Optional<Target> targetOpt = targetRepository.findActiveTargetForTerritoryAndMonth(territoryId, year, month);
        if (targetOpt.isEmpty()) {
            throw new RuntimeException("No active target found for territory and month");
        }
        
        Target target = targetOpt.get();
        
        BigDecimal coveredAmount = depositRepository.getTotalCoveredAmountForTerritoryAndMonth(territoryId, year, month);
        if (coveredAmount == null) {
            coveredAmount = BigDecimal.ZERO;
        }
        
        BigDecimal coveragePercentage = BigDecimal.ZERO;
        if (target.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            coveragePercentage = coveredAmount
                    .divide(target.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        String status = coveredAmount.compareTo(target.getTargetAmount()) >= 0 ? "ACHIEVED" : "NOT_ACHIEVED";
        
        Optional<TargetCoverage> coverageOpt = coverageRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
        TargetCoverage coverage;
        
        if (coverageOpt.isPresent()) {
            coverage = coverageOpt.get();
            coverage.setTargetAmount(target.getTargetAmount());
            coverage.setCoveredAmount(coveredAmount);
            coverage.setCoveragePercentage(coveragePercentage);
            coverage.setStatus(status);
        } else {
            coverage = new TargetCoverage();
            coverage.setTargetId(target.getId());
            coverage.setTerritoryId(territoryId);
            coverage.setYear(year);
            coverage.setMonth(month);
            coverage.setTargetAmount(target.getTargetAmount());
            coverage.setCoveredAmount(coveredAmount);
            coverage.setCoveragePercentage(coveragePercentage);
            coverage.setStatus(status);
        }
        
        return coverageRepository.save(coverage);
    }

    public void calculateCoverageSafely(UUID territoryId, Integer year, Integer month) {
        try {
            requiresNewTransactionTemplate.execute(status -> {
                calculateCoverage(territoryId, year, month);
                return null;
            });
        } catch (Exception e) {
            log.warn("Target coverage calculation skipped: {}", e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public List<TargetCoverage> getCoverageByTerritory(UUID territoryId, Integer year) {
        log.debug("Fetching coverage for territory: {}, year: {}", territoryId, year);
        return coverageRepository.findByTerritoryAndYear(territoryId, year);
    }
    
    @Transactional(readOnly = true)
    public Optional<TargetCoverage> getCoverageByTerritoryAndMonth(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching coverage for territory: {}, year: {}, month: {}", territoryId, year, month);
        return coverageRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }
}

