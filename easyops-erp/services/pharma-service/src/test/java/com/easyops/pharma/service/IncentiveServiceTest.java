package com.easyops.pharma.service;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import com.easyops.pharma.entity.IncentiveCalculation;
import com.easyops.pharma.entity.IncentiveDistribution;
import com.easyops.pharma.entity.Target;
import com.easyops.pharma.repository.*;
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
 * Phase 5: Unit tests for IncentiveService - territory-specific allocation logic.
 */
@ExtendWith(MockitoExtension.class)
class IncentiveServiceTest {

    @Mock
    private IncentiveCalculationRepository calculationRepository;
    @Mock
    private IncentiveDistributionRepository distributionRepository;
    @Mock
    private TargetService targetService;
    @Mock
    private DepositService depositService;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private EmployeeTerritoryAssignmentRepository assignmentRepository;
    @Mock
    private IncentiveRuleService incentiveRuleService;
    @Mock
    private AccountingIntegrationService accountingIntegrationService;
    @Mock
    private HrIntegrationService hrIntegrationService;

    private IncentiveService incentiveService;

    private UUID orgId;
    private UUID territoryId;
    private UUID srEmployeeId;
    private UUID mpo1Id;
    private UUID mpo2Id;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        incentiveService = new IncentiveService(
                calculationRepository, distributionRepository, targetService, depositService,
                expenseService, assignmentRepository, incentiveRuleService,
                accountingIntegrationService, hrIntegrationService);
        orgId = UUID.randomUUID();
        territoryId = UUID.randomUUID();
        srEmployeeId = UUID.randomUUID();
        mpo1Id = UUID.randomUUID();
        mpo2Id = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @Test
    void calculateIncentive_usesAllocationsAndCreatesCorrectDistributions() {
        // Given: target achieved, expenses within limit, rule with dedicated SR and allocations
        Target target = new Target();
        target.setOrganizationId(orgId);
        target.setTerritoryId(territoryId);
        target.setTargetAmount(new BigDecimal("100000"));
        when(targetService.getActiveTargetForTerritoryAndMonth(territoryId, 2025, 3))
                .thenReturn(Optional.of(target));
        when(depositService.getTotalCoveredAmount(territoryId, 2025, 3)).thenReturn(new BigDecimal("120000"));
        when(expenseService.getTotalExpensesForTerritoryAndMonth(territoryId, 2025, 3)).thenReturn(new BigDecimal("20000"));
        when(calculationRepository.findByTerritoryIdAndYearAndMonth(territoryId, 2025, 3)).thenReturn(Optional.empty());

        // SR assignment
        EmployeeTerritoryAssignment srAssignment = new EmployeeTerritoryAssignment();
        srAssignment.setEmployeeId(srEmployeeId);
        srAssignment.setRoleInTerritory("SR");
        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(srAssignment));

        // Rule: 4% incentive, 9% SR, 1% dev fund, dedicated SR, allocations sum 100%
        IncentiveRuleService.TerritoryIncentiveRuleDTO rule = new IncentiveRuleService.TerritoryIncentiveRuleDTO(
                new BigDecimal("0.04"),
                new BigDecimal("0.09"),
                new BigDecimal("0.01"),
                true,
                null,
                null,
                null,
                new BigDecimal("0.30"),
                true,
                UUID.randomUUID(),
                territoryId,
                List.of(
                        new IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO(managerId, "AM", new BigDecimal("25")),
                        new IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO(mpo1Id, "MPO", new BigDecimal("40")),
                        new IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO(mpo2Id, "MPO", new BigDecimal("35"))
                )
        );
        when(incentiveRuleService.getIncentiveRuleForTerritory(eq(territoryId), any(LocalDate.class))).thenReturn(rule);

        when(calculationRepository.save(any(IncentiveCalculation.class))).thenAnswer(inv -> {
            IncentiveCalculation calc = inv.getArgument(0);
            calc.setId(UUID.randomUUID());
            return calc;
        });
        when(distributionRepository.save(any(IncentiveDistribution.class))).thenAnswer(inv -> {
            IncentiveDistribution d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        // When
        IncentiveCalculation result = incentiveService.calculateIncentive(territoryId, 2025, 3);

        // Then: incentive base = 120000 * 0.04 = 4800
        assertThat(result.getIncentiveBaseAmount()).isEqualByComparingTo("4800");
        assertThat(result.getTerritoryEligible()).isTrue();

        // SR share = 4800 * 0.09 = 432
        assertThat(result.getTotalSrShare()).isEqualByComparingTo("432");
        // Dev fund = 4800 * 0.01 = 48
        BigDecimal remaining = new BigDecimal("4800").subtract(new BigDecimal("432")).subtract(new BigDecimal("48"));
        assertThat(remaining).isEqualByComparingTo("4320");

        // Allocations: 25% + 40% + 35% = 100% of remaining
        assertThat(result.getTotalMpoShare()).isEqualByComparingTo("3240"); // 40% + 35% of 4320
        assertThat(result.getTotalManagerShare()).isEqualByComparingTo("1080"); // 25% of 4320

        // Total distributed = SR + Dev fund + MPO + Manager
        assertThat(result.getTotalIncentiveDistributed()).isEqualByComparingTo("4800");

        // Verify distributions saved (SR_SHARE, DEVELOPMENT_FUND, 3 allocations)
        verify(distributionRepository, times(5)).save(any(IncentiveDistribution.class));
    }

    @Test
    void calculateIncentive_dualRoleMpoGetsSrAndMpoShare() {
        Target target = new Target();
        target.setOrganizationId(orgId);
        target.setTerritoryId(territoryId);
        target.setTargetAmount(new BigDecimal("100000"));
        when(targetService.getActiveTargetForTerritoryAndMonth(territoryId, 2025, 3)).thenReturn(Optional.of(target));
        when(depositService.getTotalCoveredAmount(territoryId, 2025, 3)).thenReturn(new BigDecimal("100000"));
        when(expenseService.getTotalExpensesForTerritoryAndMonth(territoryId, 2025, 3)).thenReturn(BigDecimal.ZERO);
        when(calculationRepository.findByTerritoryIdAndYearAndMonth(territoryId, 2025, 3)).thenReturn(Optional.empty());

        EmployeeTerritoryAssignment mpoAssignment = new EmployeeTerritoryAssignment();
        mpoAssignment.setEmployeeId(mpo1Id);
        mpoAssignment.setRoleInTerritory("MPO");
        when(assignmentRepository.findActiveAssignmentsByTerritory(eq(territoryId), any(LocalDate.class)))
                .thenReturn(List.of(mpoAssignment));

        // Rule: no dedicated SR, dual-role MPO = mpo1Id, allocation 100% to that MPO
        IncentiveRuleService.TerritoryIncentiveRuleDTO rule = new IncentiveRuleService.TerritoryIncentiveRuleDTO(
                new BigDecimal("0.04"),
                new BigDecimal("0.09"),
                new BigDecimal("0.01"),
                false,
                mpo1Id,
                null,
                null,
                new BigDecimal("0.30"),
                true,
                UUID.randomUUID(),
                territoryId,
                List.of(new IncentiveRuleService.TerritoryIncentiveRuleDTO.AllocationDTO(mpo1Id, "MPO", new BigDecimal("100")))
        );
        when(incentiveRuleService.getIncentiveRuleForTerritory(eq(territoryId), any(LocalDate.class))).thenReturn(rule);

        when(calculationRepository.save(any(IncentiveCalculation.class))).thenAnswer(inv -> {
            IncentiveCalculation calc = inv.getArgument(0);
            calc.setId(UUID.randomUUID());
            return calc;
        });
        when(distributionRepository.save(any(IncentiveDistribution.class))).thenAnswer(inv -> {
            IncentiveDistribution d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        IncentiveCalculation result = incentiveService.calculateIncentive(territoryId, 2025, 3);

        // incentive base = 100000 * 0.04 = 4000
        assertThat(result.getIncentiveBaseAmount()).isEqualByComparingTo("4000");
        // SR share = 4000 * 0.09 = 360, Dev fund = 40, remaining = 3600
        assertThat(result.getTotalSrShare()).isEqualByComparingTo("360");
        assertThat(result.getTotalMpoShare()).isEqualByComparingTo("3600"); // 100% of remaining
        // Dual-role MPO gets SR_SHARE + MPO_SHARE = 2 distributions for same employee
        verify(distributionRepository, times(3)).save(any(IncentiveDistribution.class)); // SR + Dev fund + MPO
    }

    @Test
    void calculateIncentive_throwsWhenNoRule() {
        when(targetService.getActiveTargetForTerritoryAndMonth(territoryId, 2025, 3)).thenReturn(Optional.of(new Target()));
        when(depositService.getTotalCoveredAmount(territoryId, 2025, 3)).thenReturn(BigDecimal.ONE);
        when(expenseService.getTotalExpensesForTerritoryAndMonth(territoryId, 2025, 3)).thenReturn(BigDecimal.ZERO);
        when(calculationRepository.findByTerritoryIdAndYearAndMonth(territoryId, 2025, 3)).thenReturn(Optional.empty());
        when(incentiveRuleService.getIncentiveRuleForTerritory(eq(territoryId), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Incentive rule is required for territory"));

        assertThatThrownBy(() -> incentiveService.calculateIncentive(territoryId, 2025, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incentive rule is required");
    }
}
