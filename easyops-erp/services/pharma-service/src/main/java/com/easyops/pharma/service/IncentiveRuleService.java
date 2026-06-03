package com.easyops.pharma.service;

import com.easyops.pharma.dto.TerritoryIncentiveRuleRequest;
import com.easyops.pharma.dto.TerritoryIncentiveRuleResponse;
import com.easyops.pharma.exception.IncentiveRuleValidationException;
import com.easyops.pharma.entity.TerritoryIncentiveAllocation;
import com.easyops.pharma.entity.TerritoryIncentiveRule;
import com.easyops.pharma.repository.EmployeeTerritoryAssignmentRepository;
import com.easyops.pharma.repository.TerritoryIncentiveAllocationRepository;
import com.easyops.pharma.repository.TerritoryIncentiveRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncentiveRuleService {

    private static final BigDecimal ALLOCATION_SUM_REQUIRED = new BigDecimal("100.00");

    private final TerritoryIncentiveRuleRepository ruleRepository;
    private final TerritoryIncentiveAllocationRepository allocationRepository;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;

    /**
     * Get active incentive rule for territory at specific date (defaults to today).
     * Throws if no territory-specific rule exists (no general/default rule).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "territoryIncentiveRules", key = "#territoryId + '_' + #date")
    public TerritoryIncentiveRuleDTO getIncentiveRuleForTerritory(UUID territoryId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        TerritoryIncentiveRule rule = ruleRepository.findActiveRuleForTerritoryAndDate(territoryId, date)
                .orElseThrow(() -> new RuntimeException("Incentive rule is required for territory " + territoryId + ". Please define a territory-specific rule."));

        List<TerritoryIncentiveAllocation> allocations = allocationRepository.findByTerritoryIncentiveRuleId(rule.getId());
        return toDTO(rule, allocations);
    }

    private TerritoryIncentiveRuleDTO toDTO(TerritoryIncentiveRule rule, List<TerritoryIncentiveAllocation> allocations) {
        List<TerritoryIncentiveRuleDTO.AllocationDTO> allocationDTOs = allocations.stream()
                .map(a -> new TerritoryIncentiveRuleDTO.AllocationDTO(a.getEmployeeId(), a.getRoleInTerritory(), a.getAllocationPercentage()))
                .toList();
        return new TerritoryIncentiveRuleDTO(
                rule.getIncentivePercentage(),
                rule.getSrSharePercentage(),
                rule.getDevelopmentFundPercentage() != null ? rule.getDevelopmentFundPercentage() : new BigDecimal("0.01"),
                rule.getHasDedicatedSr() != null ? rule.getHasDedicatedSr() : true,
                rule.getDualRoleEmployeeId(),
                rule.getMpoSharePercentage(),
                rule.getManagerSharePercentage(),
                rule.getExpenseLimitPercentage(),
                true,
                rule.getId(),
                rule.getTerritoryId(),
                allocationDTOs
        );
    }
    
    /**
     * Create or update territory incentive rule with allocations.
     * Validates: allocation sum = 100%, has_dedicated_sr or dual_role_employee_id set, dual-role employee in territory.
     */
    @Transactional
    @CacheEvict(value = "territoryIncentiveRules", allEntries = true)
    public TerritoryIncentiveRule validateAndSaveRule(TerritoryIncentiveRule rule, List<TerritoryIncentiveAllocation> allocations) {
        log.info("Validating and saving incentive rule for territory: {}", rule.getTerritoryId());

        validateRule(rule, allocations);
        TerritoryIncentiveRule savedRule = saveRule(rule);

        allocationRepository.deleteByTerritoryIncentiveRuleId(savedRule.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (TerritoryIncentiveAllocation a : allocations) {
                a.setTerritoryIncentiveRuleId(savedRule.getId());
                a.setId(null);
                allocationRepository.save(a);
            }
        }
        return savedRule;
    }

    /**
     * Create or update territory incentive rule from request (with allocations).
     */
    @Transactional
    @CacheEvict(value = "territoryIncentiveRules", allEntries = true)
    public TerritoryIncentiveRuleResponse validateAndSaveRuleFromRequest(TerritoryIncentiveRuleRequest request) {
        TerritoryIncentiveRule rule = toEntity(request);
        List<TerritoryIncentiveAllocation> allocations = toAllocations(request.getAllocations());
        TerritoryIncentiveRule savedRule = validateAndSaveRule(rule, allocations);
        return toResponse(savedRule, allocationRepository.findByTerritoryIncentiveRuleId(savedRule.getId()));
    }

    /**
     * Get rule with allocations as response DTO for territory.
     */
    @Transactional(readOnly = true)
    public Optional<TerritoryIncentiveRuleResponse> getRuleResponseForTerritory(UUID territoryId) {
        return ruleRepository.findActiveRuleForTerritoryAndDate(territoryId, LocalDate.now())
                .map(rule -> toResponse(rule, allocationRepository.findByTerritoryIncentiveRuleId(rule.getId())));
    }

    /**
     * Get allocations for territory's active rule.
     */
    @Transactional(readOnly = true)
    public List<TerritoryIncentiveRuleResponse.AllocationItem> getAllocationsForTerritory(UUID territoryId) {
        Optional<TerritoryIncentiveRule> ruleOpt = ruleRepository.findActiveRuleForTerritoryAndDate(territoryId, LocalDate.now());
        if (ruleOpt.isEmpty()) {
            return List.of();
        }
        return allocationRepository.findByTerritoryIncentiveRuleId(ruleOpt.get().getId()).stream()
                .map(a -> new TerritoryIncentiveRuleResponse.AllocationItem(a.getId(), a.getEmployeeId(), a.getRoleInTerritory(), a.getAllocationPercentage()))
                .toList();
    }

    private TerritoryIncentiveRule toEntity(TerritoryIncentiveRuleRequest request) {
        TerritoryIncentiveRule rule = new TerritoryIncentiveRule();
        rule.setId(request.getId());
        rule.setOrganizationId(request.getOrganizationId());
        rule.setTerritoryId(request.getTerritoryId());
        rule.setIncentivePercentage(request.getIncentivePercentage() != null ? request.getIncentivePercentage() : new BigDecimal("0.04"));
        rule.setSrSharePercentage(request.getSrSharePercentage() != null ? request.getSrSharePercentage() : new BigDecimal("0.09"));
        rule.setDevelopmentFundPercentage(request.getDevelopmentFundPercentage() != null ? request.getDevelopmentFundPercentage() : new BigDecimal("0.01"));
        rule.setHasDedicatedSr(request.getHasDedicatedSr() != null ? request.getHasDedicatedSr() : true);
        rule.setDualRoleEmployeeId(request.getDualRoleEmployeeId());
        rule.setExpenseLimitPercentage(request.getExpenseLimitPercentage() != null ? request.getExpenseLimitPercentage() : new BigDecimal("0.30"));
        rule.setEffectiveFromDate(request.getEffectiveFromDate());
        rule.setEffectiveToDate(request.getEffectiveToDate());
        rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        rule.setDescription(request.getDescription());
        rule.setNotes(request.getNotes());
        return rule;
    }

    private List<TerritoryIncentiveAllocation> toAllocations(List<TerritoryIncentiveRuleRequest.AllocationItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(a -> {
                    TerritoryIncentiveAllocation alloc = new TerritoryIncentiveAllocation();
                    alloc.setEmployeeId(a.getEmployeeId());
                    alloc.setRoleInTerritory(a.getRoleInTerritory());
                    alloc.setAllocationPercentage(a.getAllocationPercentage());
                    return alloc;
                })
                .toList();
    }

    private TerritoryIncentiveRuleResponse toResponse(TerritoryIncentiveRule rule, List<TerritoryIncentiveAllocation> allocations) {
        TerritoryIncentiveRuleResponse response = new TerritoryIncentiveRuleResponse();
        response.setId(rule.getId());
        response.setOrganizationId(rule.getOrganizationId());
        response.setTerritoryId(rule.getTerritoryId());
        response.setIncentivePercentage(rule.getIncentivePercentage());
        response.setSrSharePercentage(rule.getSrSharePercentage());
        response.setDevelopmentFundPercentage(rule.getDevelopmentFundPercentage() != null ? rule.getDevelopmentFundPercentage() : new BigDecimal("0.01"));
        response.setHasDedicatedSr(rule.getHasDedicatedSr() != null ? rule.getHasDedicatedSr() : true);
        response.setDualRoleEmployeeId(rule.getDualRoleEmployeeId());
        response.setExpenseLimitPercentage(rule.getExpenseLimitPercentage());
        response.setRuleVersion(rule.getRuleVersion());
        response.setEffectiveFromDate(rule.getEffectiveFromDate());
        response.setEffectiveToDate(rule.getEffectiveToDate());
        response.setIsActive(rule.getIsActive());
        response.setDescription(rule.getDescription());
        response.setNotes(rule.getNotes());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        response.setAllocations(allocations.stream()
                .map(a -> new TerritoryIncentiveRuleResponse.AllocationItem(a.getId(), a.getEmployeeId(), a.getRoleInTerritory(), a.getAllocationPercentage()))
                .toList());
        response.setValidationStatus("VALID");
        return response;
    }

    private void validateRule(TerritoryIncentiveRule rule, List<TerritoryIncentiveAllocation> allocations) {
        if (Boolean.FALSE.equals(rule.getHasDedicatedSr()) && rule.getDualRoleEmployeeId() == null) {
            throw new IncentiveRuleValidationException("Territory has no SR designation. Please designate one MPO to additionally act as SR (dual role).");
        }
        if (rule.getDualRoleEmployeeId() != null) {
            boolean inTerritory = assignmentRepository.findActiveAssignmentsByTerritory(rule.getTerritoryId(), LocalDate.now()).stream()
                    .anyMatch(a -> a.getEmployeeId().equals(rule.getDualRoleEmployeeId()));
            if (!inTerritory) {
                throw new IncentiveRuleValidationException("Dual-role employee must be assigned to this territory.");
            }
        }
        if (allocations != null && !allocations.isEmpty()) {
            BigDecimal sum = allocations.stream()
                    .map(TerritoryIncentiveAllocation::getAllocationPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(ALLOCATION_SUM_REQUIRED) != 0) {
                throw new IncentiveRuleValidationException("Sum of Manager and MPO allocation percentages must equal 100%. Current sum: " + sum.setScale(2, RoundingMode.HALF_UP) + "%");
            }
            // Dual-role employee must have allocation record when hasDedicatedSr=false
            if (Boolean.FALSE.equals(rule.getHasDedicatedSr()) && rule.getDualRoleEmployeeId() != null) {
                boolean dualRoleInAllocations = allocations.stream()
                        .anyMatch(a -> rule.getDualRoleEmployeeId().equals(a.getEmployeeId()));
                if (!dualRoleInAllocations) {
                    throw new IncentiveRuleValidationException("Dual-role employee must have an allocation record in the allocations list.");
                }
            }
            // All allocation employees must be in territory
            Set<UUID> territoryEmployeeIds = assignmentRepository.findActiveAssignmentsByTerritory(rule.getTerritoryId(), LocalDate.now()).stream()
                    .map(a -> a.getEmployeeId())
                    .collect(Collectors.toSet());
            for (TerritoryIncentiveAllocation a : allocations) {
                if (!territoryEmployeeIds.contains(a.getEmployeeId())) {
                    throw new IncentiveRuleValidationException("Employee " + a.getEmployeeId() + " is not assigned to this territory. All allocation employees must be in the territory.");
                }
            }
        }
    }

    private TerritoryIncentiveRule saveRule(TerritoryIncentiveRule rule) {
        log.info("Saving incentive rule for territory: {}", rule.getTerritoryId());

        Optional<TerritoryIncentiveRule> existingOpt = ruleRepository.findByTerritoryIdAndIsActive(rule.getTerritoryId(), true);

        if (existingOpt.isPresent() && !existingOpt.get().getId().equals(rule.getId())) {
            TerritoryIncentiveRule oldRule = existingOpt.get();
            oldRule.setIsActive(false);
            ruleRepository.save(oldRule);

            List<TerritoryIncentiveRule> history = ruleRepository.findHistoryByTerritoryId(rule.getTerritoryId());
            int maxVersion = history.stream()
                    .mapToInt(TerritoryIncentiveRule::getRuleVersion)
                    .max()
                    .orElse(0);
            rule.setRuleVersion(maxVersion + 1);
        }

        if (rule.getEffectiveFromDate() == null) {
            rule.setEffectiveFromDate(LocalDate.now());
        }

        return ruleRepository.save(rule);
    }

    /**
     * Get allocations for a rule.
     */
    @Transactional(readOnly = true)
    public List<TerritoryIncentiveAllocation> getAllocationsForRule(UUID ruleId) {
        return allocationRepository.findByTerritoryIncentiveRuleId(ruleId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TerritoryIncentiveRule> findRuleById(UUID ruleId) {
        return ruleRepository.findById(ruleId);
    }

    /**
     * Deactivate territory incentive rule (revert to defaults)
     */
    @Transactional
    @CacheEvict(value = "territoryIncentiveRules", allEntries = true)
    public void deactivateRule(UUID ruleId) {
        log.info("Deactivating incentive rule: {}", ruleId);
        Optional<TerritoryIncentiveRule> ruleOpt = ruleRepository.findById(ruleId);
        if (ruleOpt.isPresent()) {
            TerritoryIncentiveRule rule = ruleOpt.get();
            rule.setIsActive(false);
            rule.setEffectiveToDate(LocalDate.now());
            ruleRepository.save(rule);
        }
    }
    
    /**
     * Get rule history for territory
     */
    @Transactional(readOnly = true)
    public List<TerritoryIncentiveRule> getRuleHistory(UUID territoryId) {
        return ruleRepository.findHistoryByTerritoryId(territoryId);
    }
    
    /**
     * DTO for incentive rule values
     */
    public static class TerritoryIncentiveRuleDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private final BigDecimal incentivePercentage;
        private final BigDecimal srSharePercentage;
        private final BigDecimal developmentFundPercentage;
        private final Boolean hasDedicatedSr;
        private final UUID dualRoleEmployeeId;
        private final BigDecimal mpoSharePercentage;
        private final BigDecimal managerSharePercentage;
        private final BigDecimal expenseLimitPercentage;
        private final Boolean isCustomized;
        private final UUID ruleId;
        private final UUID territoryId;
        private final List<AllocationDTO> allocations;

        public TerritoryIncentiveRuleDTO(
                BigDecimal incentivePercentage,
                BigDecimal srSharePercentage,
                BigDecimal developmentFundPercentage,
                Boolean hasDedicatedSr,
                UUID dualRoleEmployeeId,
                BigDecimal mpoSharePercentage,
                BigDecimal managerSharePercentage,
                BigDecimal expenseLimitPercentage,
                Boolean isCustomized,
                UUID ruleId,
                UUID territoryId,
                List<AllocationDTO> allocations) {
            this.incentivePercentage = incentivePercentage;
            this.srSharePercentage = srSharePercentage;
            this.developmentFundPercentage = developmentFundPercentage != null ? developmentFundPercentage : new BigDecimal("0.01");
            this.hasDedicatedSr = hasDedicatedSr != null ? hasDedicatedSr : true;
            this.dualRoleEmployeeId = dualRoleEmployeeId;
            this.mpoSharePercentage = mpoSharePercentage;
            this.managerSharePercentage = managerSharePercentage;
            this.expenseLimitPercentage = expenseLimitPercentage;
            this.isCustomized = isCustomized;
            this.ruleId = ruleId;
            this.territoryId = territoryId;
            this.allocations = allocations != null ? allocations : new ArrayList<>();
        }

        public record AllocationDTO(UUID employeeId, String roleInTerritory, BigDecimal allocationPercentage) implements Serializable {}

        public BigDecimal getIncentivePercentage() { return incentivePercentage; }
        public BigDecimal getSrSharePercentage() { return srSharePercentage; }
        public BigDecimal getDevelopmentFundPercentage() { return developmentFundPercentage; }
        public Boolean getHasDedicatedSr() { return hasDedicatedSr; }
        public UUID getDualRoleEmployeeId() { return dualRoleEmployeeId; }
        public BigDecimal getMpoSharePercentage() { return mpoSharePercentage; }
        public BigDecimal getManagerSharePercentage() { return managerSharePercentage; }
        public BigDecimal getExpenseLimitPercentage() { return expenseLimitPercentage; }
        public Boolean getIsCustomized() { return isCustomized; }
        public UUID getRuleId() { return ruleId; }
        public UUID getTerritoryId() { return territoryId; }
        public List<AllocationDTO> getAllocations() { return allocations; }
    }
}
