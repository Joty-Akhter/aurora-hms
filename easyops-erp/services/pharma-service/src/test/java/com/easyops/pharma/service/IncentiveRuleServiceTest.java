package com.easyops.pharma.service;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import com.easyops.pharma.entity.TerritoryIncentiveAllocation;
import com.easyops.pharma.entity.TerritoryIncentiveRule;
import com.easyops.pharma.exception.IncentiveRuleValidationException;
import com.easyops.pharma.repository.EmployeeTerritoryAssignmentRepository;
import com.easyops.pharma.repository.TerritoryIncentiveAllocationRepository;
import com.easyops.pharma.repository.TerritoryIncentiveRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Phase 6: Unit tests for IncentiveRuleService validation.
 */
@ExtendWith(MockitoExtension.class)
class IncentiveRuleServiceTest {

    @Mock
    private TerritoryIncentiveRuleRepository ruleRepository;
    @Mock
    private TerritoryIncentiveAllocationRepository allocationRepository;
    @Mock
    private EmployeeTerritoryAssignmentRepository assignmentRepository;

    private IncentiveRuleService incentiveRuleService;

    private UUID orgId;
    private UUID territoryId;
    private UUID mpo1Id;
    private UUID mpo2Id;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        incentiveRuleService = new IncentiveRuleService(ruleRepository, allocationRepository, assignmentRepository);
        orgId = UUID.randomUUID();
        territoryId = UUID.randomUUID();
        mpo1Id = UUID.randomUUID();
        mpo2Id = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @Test
    void validateRule_throwsWhenNoDedicatedSrAndNoDualRoleEmployee() {
        TerritoryIncentiveRule rule = createRule(false, null);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(mpo1Id, "MPO", new BigDecimal("100"))
        );

        assertThatThrownBy(() -> incentiveRuleService.validateAndSaveRule(rule, allocations))
                .isInstanceOf(IncentiveRuleValidationException.class)
                .hasMessageContaining("designate one MPO to additionally act as SR");
    }

    @Test
    void validateRule_throwsWhenAllocationSumNot100() {
        TerritoryIncentiveRule rule = createRule(true, null);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(mpo1Id, "MPO", new BigDecimal("50")),
                createAllocation(mpo2Id, "MPO", new BigDecimal("40"))
        );

        assertThatThrownBy(() -> incentiveRuleService.validateAndSaveRule(rule, allocations))
                .isInstanceOf(IncentiveRuleValidationException.class)
                .hasMessageContaining("must equal 100%");
    }

    @Test
    void validateRule_throwsWhenDualRoleEmployeeNotInTerritory() {
        TerritoryIncentiveRule rule = createRule(false, mpo1Id);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(mpo2Id, "MPO", new BigDecimal("100"))
        );

        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(createAssignment(mpo2Id, "MPO")));

        assertThatThrownBy(() -> incentiveRuleService.validateAndSaveRule(rule, allocations))
                .isInstanceOf(IncentiveRuleValidationException.class)
                .hasMessageContaining("Dual-role employee must be assigned");
    }

    @Test
    void validateRule_throwsWhenDualRoleEmployeeNotInAllocations() {
        TerritoryIncentiveRule rule = createRule(false, mpo1Id);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(mpo2Id, "MPO", new BigDecimal("100"))
        );

        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(createAssignment(mpo1Id, "MPO"), createAssignment(mpo2Id, "MPO")));

        assertThatThrownBy(() -> incentiveRuleService.validateAndSaveRule(rule, allocations))
                .isInstanceOf(IncentiveRuleValidationException.class)
                .hasMessageContaining("Dual-role employee must have an allocation record");
    }

    @Test
    void validateRule_throwsWhenAllocationEmployeeNotInTerritory() {
        UUID outsiderId = UUID.randomUUID();
        TerritoryIncentiveRule rule = createRule(true, null);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(outsiderId, "MPO", new BigDecimal("100"))
        );

        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(createAssignment(mpo1Id, "MPO")));

        assertThatThrownBy(() -> incentiveRuleService.validateAndSaveRule(rule, allocations))
                .isInstanceOf(IncentiveRuleValidationException.class)
                .hasMessageContaining("not assigned to this territory");
    }

    @Test
    void validateRule_succeedsWithValidRuleAndAllocations() {
        TerritoryIncentiveRule rule = createRule(true, null);
        List<TerritoryIncentiveAllocation> allocations = List.of(
                createAllocation(managerId, "AM", new BigDecimal("25")),
                createAllocation(mpo1Id, "MPO", new BigDecimal("40")),
                createAllocation(mpo2Id, "MPO", new BigDecimal("35"))
        );

        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(
                        createAssignment(managerId, "AM"),
                        createAssignment(mpo1Id, "MPO"),
                        createAssignment(mpo2Id, "MPO")
                ));
        when(ruleRepository.findByTerritoryIdAndIsActive(territoryId, true)).thenReturn(Optional.empty());
        when(ruleRepository.save(any())).thenAnswer(inv -> {
            TerritoryIncentiveRule r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TerritoryIncentiveRule saved = incentiveRuleService.validateAndSaveRule(rule, allocations);

        assertThat(saved).isNotNull();
        assertThat(saved.getTerritoryId()).isEqualTo(territoryId);
        verify(ruleRepository).save(any());
        verify(allocationRepository).deleteByTerritoryIncentiveRuleId(any());
        verify(allocationRepository, times(3)).save(any());
    }

    private TerritoryIncentiveRule createRule(boolean hasDedicatedSr, UUID dualRoleEmployeeId) {
        TerritoryIncentiveRule rule = new TerritoryIncentiveRule();
        rule.setOrganizationId(orgId);
        rule.setTerritoryId(territoryId);
        rule.setIncentivePercentage(new BigDecimal("0.04"));
        rule.setSrSharePercentage(new BigDecimal("0.09"));
        rule.setDevelopmentFundPercentage(new BigDecimal("0.01"));
        rule.setHasDedicatedSr(hasDedicatedSr);
        rule.setDualRoleEmployeeId(dualRoleEmployeeId);
        rule.setExpenseLimitPercentage(new BigDecimal("0.30"));
        rule.setIsActive(true);
        return rule;
    }

    private TerritoryIncentiveAllocation createAllocation(UUID employeeId, String role, BigDecimal pct) {
        TerritoryIncentiveAllocation a = new TerritoryIncentiveAllocation();
        a.setEmployeeId(employeeId);
        a.setRoleInTerritory(role);
        a.setAllocationPercentage(pct);
        return a;
    }

    private EmployeeTerritoryAssignment createAssignment(UUID employeeId, String role) {
        EmployeeTerritoryAssignment a = new EmployeeTerritoryAssignment();
        a.setEmployeeId(employeeId);
        a.setRoleInTerritory(role);
        a.setTerritoryId(territoryId);
        return a;
    }
}
