package com.easyops.pharma.controller;

import com.easyops.pharma.entity.Deposit;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.DepositService;
import com.easyops.pharma.service.SoldProductEntryService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/deposits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Deposit Management", description = "Territory-wise deposit and collection management APIs")
@CrossOrigin(origins = "*")
public class DepositController {

    private final DepositService depositService;
    private final SoldProductEntryService soldProductEntryService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all deposits")
    public ResponseEntity<List<Deposit>> getAllDeposits(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/deposits - organizationId: {}", organizationId);
        List<Deposit> deposits = depositService.getAllDeposits(organizationId);
        return ResponseEntity.ok(deposits);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get deposits by territory")
    public ResponseEntity<List<Deposit>> getDepositsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/deposits/territory/{}", territoryId);
        List<Deposit> deposits = depositService.getDepositsByTerritory(territoryId);
        return ResponseEntity.ok(deposits);
    }

    @GetMapping("/territory/{territoryId}/period")
    @Operation(summary = "Get deposits by territory and period")
    public ResponseEntity<List<Deposit>> getDepositsByTerritoryAndPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/deposits/territory/{}/period - year: {}, month: {}", territoryId, year, month);
        List<Deposit> deposits = depositService.getDepositsByTerritoryAndPeriod(territoryId, year, month);
        return ResponseEntity.ok(deposits);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deposit by ID")
    public ResponseEntity<Deposit> getDepositById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Deposit deposit = depositService.getDepositById(id);
        pharmaRbac.requirePharmaView(actor, deposit.getOrganizationId());
        log.info("GET /api/pharma/deposits/{}", id);
        return ResponseEntity.ok(deposit);
    }

    @GetMapping("/territory/{territoryId}/covered-amount")
    @Operation(summary = "Get total covered amount (from sold product entries) for territory and month")
    public ResponseEntity<BigDecimal> getTotalCoveredAmount(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/deposits/territory/{}/covered-amount - year: {}, month: {}", territoryId, year, month);
        BigDecimal total = soldProductEntryService.getTotalCoveredAmount(territoryId, year, month);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/territory/{territoryId}/outstanding-quantity")
    @Operation(summary = "Get outstanding quantity for a product in a territory")
    public ResponseEntity<BigDecimal> getOutstandingQuantity(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId,
            @RequestParam("productId") UUID productId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/deposits/territory/{}/outstanding-quantity - productId: {}", territoryId, productId);
        BigDecimal quantity = soldProductEntryService.getOutstandingQuantity(territoryId, productId);
        return ResponseEntity.ok(quantity);
    }

    @PostMapping
    @Operation(summary = "Create new deposit")
    public ResponseEntity<Deposit> createDeposit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Deposit deposit) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, deposit.getOrganizationId());
        log.info("POST /api/pharma/deposits");
        Deposit created = depositService.createDeposit(deposit);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update deposit")
    public ResponseEntity<Deposit> updateDeposit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Deposit deposit) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, deposit.getOrganizationId());
        log.info("PUT /api/pharma/deposits/{}", id);
        Deposit updated = depositService.updateDeposit(id, deposit);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit deposit")
    public ResponseEntity<Deposit> submitDeposit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Deposit existing = depositService.getDepositById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/deposits/{}/submit", id);
        Deposit deposit = depositService.submitDeposit(id);
        return ResponseEntity.ok(deposit);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete deposit")
    public ResponseEntity<Deposit> completeDeposit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Deposit existing = depositService.getDepositById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/deposits/{}/complete", id);
        Deposit deposit = depositService.completeDeposit(id);
        return ResponseEntity.ok(deposit);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete deposit")
    public ResponseEntity<Void> deleteDeposit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Deposit existing = depositService.getDepositById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/deposits/{}", id);
        depositService.deleteDeposit(id);
        return ResponseEntity.noContent().build();
    }
}
