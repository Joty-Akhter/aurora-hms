package com.easyops.inventory.controller;

import com.easyops.inventory.entity.ReorderAlert;
import com.easyops.inventory.entity.ReorderRule;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.ReorderRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/reorder")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reorder Management", description = "Reorder point automation and alert management APIs")
@CrossOrigin(origins = "*")
public class ReorderController {

    private final ReorderRuleService reorderRuleService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping("/rules")
    @Operation(summary = "Get all reorder rules")
    public ResponseEntity<List<ReorderRule>> getAllRules(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reorder/rules - org: {}, activeOnly: {}", organizationId, activeOnly);

        List<ReorderRule> rules = Boolean.TRUE.equals(activeOnly)
                ? reorderRuleService.getActiveRules(organizationId)
                : reorderRuleService.getAllRules(organizationId);

        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "Get reorder rule by ID")
    public ResponseEntity<ReorderRule> getRuleById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderRule rule = reorderRuleService.getRuleById(id);
        inventoryRbac.requireInventoryView(actor, rule.getOrganizationId());
        log.info("GET /api/inventory/reorder/rules/{}", id);
        return ResponseEntity.ok(rule);
    }

    @PostMapping("/rules")
    @Operation(summary = "Create new reorder rule")
    public ResponseEntity<ReorderRule> createRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ReorderRule rule) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, rule.getOrganizationId());
        log.info("POST /api/inventory/reorder/rules - product: {}, warehouse: {}",
                rule.getProductId(), rule.getWarehouseId());
        ReorderRule created = reorderRuleService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "Update reorder rule")
    public ResponseEntity<ReorderRule> updateRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody ReorderRule rule) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderRule existing = reorderRuleService.getRuleById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("PUT /api/inventory/reorder/rules/{}", id);
        ReorderRule updated = reorderRuleService.updateRule(id, rule);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "Delete reorder rule")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderRule existing = reorderRuleService.getRuleById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/inventory/reorder/rules/{}", id);
        reorderRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rules/{id}/check")
    @Operation(summary = "Manually check reorder rule now")
    public ResponseEntity<Void> checkRuleNow(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderRule existing = reorderRuleService.getRuleById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/reorder/rules/{}/check", id);
        reorderRuleService.checkRuleNow(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get all reorder alerts")
    public ResponseEntity<List<ReorderAlert>> getAllAlerts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean openOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reorder/alerts - org: {}, openOnly: {}", organizationId, openOnly);

        List<ReorderAlert> alerts = Boolean.TRUE.equals(openOnly)
                ? reorderRuleService.getOpenAlerts(organizationId)
                : reorderRuleService.getAllAlerts(organizationId);

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<ReorderAlert> getAlertById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderAlert alert = reorderRuleService.getAlertById(id);
        inventoryRbac.requireInventoryView(actor, alert.getOrganizationId());
        log.info("GET /api/inventory/reorder/alerts/{}", id);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @Operation(summary = "Acknowledge reorder alert")
    public ResponseEntity<ReorderAlert> acknowledgeAlert(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderAlert existing = reorderRuleService.getAlertById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/reorder/alerts/{}/acknowledge", id);

        UUID userId = UUID.fromString(request.get("userId").toString());
        ReorderAlert alert = reorderRuleService.acknowledgeAlert(id, userId);

        return ResponseEntity.ok(alert);
    }

    @PostMapping("/alerts/{id}/close")
    @Operation(summary = "Close reorder alert")
    public ResponseEntity<ReorderAlert> closeAlert(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ReorderAlert existing = reorderRuleService.getAlertById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("POST /api/inventory/reorder/alerts/{}/close", id);

        String notes = request != null && request.containsKey("notes")
                ? request.get("notes").toString()
                : null;

        ReorderAlert alert = reorderRuleService.closeAlert(id, notes);
        return ResponseEntity.ok(alert);
    }
}
