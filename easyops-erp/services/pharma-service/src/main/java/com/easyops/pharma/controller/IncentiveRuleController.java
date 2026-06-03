package com.easyops.pharma.controller;

import com.easyops.pharma.dto.TerritoryIncentiveRuleRequest;
import com.easyops.pharma.dto.TerritoryIncentiveRuleResponse;
import com.easyops.pharma.entity.TerritoryIncentiveRule;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.IncentiveRuleService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/incentive-rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incentive Rule Management", description = "Territory-specific incentive rule management APIs")
@CrossOrigin(origins = "*")
public class IncentiveRuleController {

    private final IncentiveRuleService incentiveRuleService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get active incentive rule for territory", description = "Returns rule + allocations; 404 if no rule defined")
    public ResponseEntity<TerritoryIncentiveRuleResponse> getIncentiveRuleForTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam(name = "date", required = false) String date) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentive-rules/territory/{} - date: {}", territoryId, date);
        Optional<TerritoryIncentiveRuleResponse> ruleOpt = incentiveRuleService.getRuleResponseForTerritory(territoryId);
        return ruleOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/territory/{territoryId}/allocations")
    @Operation(summary = "List allocations for territory's active rule")
    public ResponseEntity<List<TerritoryIncentiveRuleResponse.AllocationItem>> getAllocationsForTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentive-rules/territory/{}/allocations", territoryId);
        List<TerritoryIncentiveRuleResponse.AllocationItem> allocations = incentiveRuleService.getAllocationsForTerritory(territoryId);
        return ResponseEntity.ok(allocations);
    }

    @PostMapping
    @Operation(summary = "Create or update territory incentive rule with allocations", description = "Validates allocation sum=100%, dual-role, employees in territory")
    public ResponseEntity<TerritoryIncentiveRuleResponse> createOrUpdateRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TerritoryIncentiveRuleRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(request.getTerritoryId()).getOrganizationId();
        pharmaRbac.requirePharmaManage(actor, orgId);
        log.info("POST /api/pharma/incentive-rules - territory: {}", request.getTerritoryId());
        TerritoryIncentiveRuleResponse response = incentiveRuleService.validateAndSaveRuleFromRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update territory incentive rule with allocations")
    public ResponseEntity<TerritoryIncentiveRuleResponse> updateRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody TerritoryIncentiveRuleRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        request.setId(id);
        UUID orgId = territoryService.getTerritoryById(request.getTerritoryId()).getOrganizationId();
        pharmaRbac.requirePharmaManage(actor, orgId);
        log.info("PUT /api/pharma/incentive-rules/{}", id);
        TerritoryIncentiveRuleResponse response = incentiveRuleService.validateAndSaveRuleFromRequest(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate territory incentive rule", description = "Deactivates rule to revert to defaults")
    public ResponseEntity<Void> deactivateRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        TerritoryIncentiveRule rule = incentiveRuleService.findRuleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        pharmaRbac.requirePharmaManage(actor, rule.getOrganizationId());
        log.info("DELETE /api/pharma/incentive-rules/{}", id);
        incentiveRuleService.deactivateRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/territory/{territoryId}/history")
    @Operation(summary = "Get incentive rule history for territory")
    public ResponseEntity<List<TerritoryIncentiveRule>> getRuleHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/incentive-rules/territory/{}/history", territoryId);
        List<TerritoryIncentiveRule> history = incentiveRuleService.getRuleHistory(territoryId);
        return ResponseEntity.ok(history);
    }
}
