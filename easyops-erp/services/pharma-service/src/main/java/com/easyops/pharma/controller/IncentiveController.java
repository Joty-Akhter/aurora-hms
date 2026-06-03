package com.easyops.pharma.controller;

import com.easyops.pharma.entity.IncentiveCalculation;
import com.easyops.pharma.entity.IncentiveDistribution;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.IncentiveService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/incentives")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incentive Management", description = "Territory-wise incentive calculation and distribution APIs")
@CrossOrigin(origins = "*")
public class IncentiveController {

    private final IncentiveService incentiveService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate incentive for territory and month")
    public ResponseEntity<IncentiveCalculation> calculateIncentive(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(value = "force", defaultValue = "false") boolean forceRecalculate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaManage(actor, orgId);
        log.info("POST /api/pharma/incentives/calculate - territory: {}, year: {}, month: {}, force: {}", territoryId, year, month, forceRecalculate);
        IncentiveCalculation calculation = incentiveService.calculateIncentive(territoryId, year, month, forceRecalculate);
        return ResponseEntity.ok(calculation);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get incentive calculations by territory and year")
    public ResponseEntity<List<IncentiveCalculation>> getCalculationsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentives/territory/{} - year: {}", territoryId, year);
        List<IncentiveCalculation> calculations = incentiveService.getCalculationsByTerritory(territoryId, year);
        return ResponseEntity.ok(calculations);
    }

    @GetMapping("/territory/{territoryId}/month")
    @Operation(summary = "Get incentive calculation by territory and month")
    public ResponseEntity<IncentiveCalculation> getCalculationByTerritoryAndMonth(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentives/territory/{}/month - year: {}, month: {}", territoryId, year, month);
        Optional<IncentiveCalculation> calculation = incentiveService.getCalculationByTerritoryAndMonth(territoryId, year, month);
        return calculation.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get incentive distributions by employee")
    public ResponseEntity<List<IncentiveDistribution>> getDistributionsByEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeId") UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = incentiveService.resolveOrganizationIdForEmployeeDistributions(employeeId);
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentives/employee/{}", employeeId);
        List<IncentiveDistribution> distributions = incentiveService.getDistributionsByEmployee(employeeId);
        return ResponseEntity.ok(distributions);
    }

    @PostMapping("/{calculationId}/mark-paid")
    @Operation(summary = "Mark incentives as paid")
    public ResponseEntity<Void> markIncentivesAsPaid(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("calculationId") UUID calculationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        IncentiveCalculation calculation = incentiveService.getCalculationById(calculationId);
        pharmaRbac.requirePharmaManage(actor, calculation.getOrganizationId());
        log.info("POST /api/pharma/incentives/{}/mark-paid", calculationId);
        incentiveService.markIncentivesAsPaid(calculationId);
        return ResponseEntity.ok().build();
    }
}
