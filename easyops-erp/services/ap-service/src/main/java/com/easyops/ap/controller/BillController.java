package com.easyops.ap.controller;

import com.easyops.ap.dto.BillRequest;
import com.easyops.ap.entity.APBill;
import com.easyops.ap.security.AccountingRbacService;
import com.easyops.ap.security.RbacRequestHeaders;
import com.easyops.ap.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ap/bills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AP Bills", description = "Bill management for Accounts Payable")
public class BillController {

    private final BillService billService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all bills for an organization")
    public ResponseEntity<List<APBill>> getAllBills(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        List<APBill> bills = status != null
                ? billService.getBillsByStatus(organizationId, status)
                : billService.getAllBills(organizationId);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/outstanding")
    @Operation(summary = "Get outstanding bills")
    public ResponseEntity<List<APBill>> getOutstandingBills(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(billService.getOutstandingBills(organizationId));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue bills")
    public ResponseEntity<List<APBill>> getOverdueBills(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(billService.getOverdueBills(organizationId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<APBill> getBillById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        APBill bill = billService.getBillById(id);
        accountingRbac.requireAccountingView(actor, bill.getOrganizationId());
        return ResponseEntity.ok(bill);
    }

    @PostMapping
    @Operation(summary = "Create new bill")
    public ResponseEntity<APBill> createBill(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody BillRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(request, actor));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post bill")
    public ResponseEntity<APBill> postBill(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        APBill existing = billService.getBillById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(billService.postBill(id, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bill")
    public ResponseEntity<Void> deleteBill(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        APBill existing = billService.getBillById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        billService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }
}
