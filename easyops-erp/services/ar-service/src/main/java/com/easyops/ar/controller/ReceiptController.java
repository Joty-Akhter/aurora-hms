package com.easyops.ar.controller;

import com.easyops.ar.dto.ReceiptRequest;
import com.easyops.ar.entity.ARReceipt;
import com.easyops.ar.security.AccountingRbacService;
import com.easyops.ar.security.RbacRequestHeaders;
import com.easyops.ar.service.ReceiptService;
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
@RequestMapping("/api/ar/receipts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AR Receipts", description = "Receipt management for Accounts Receivable")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all receipts for an organization")
    public ResponseEntity<List<ARReceipt>> getAllReceipts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/receipts - organizationId: {}, status: {}", organizationId, status);
        List<ARReceipt> receipts = status != null
                ? receiptService.getReceiptsByStatus(organizationId, status)
                : receiptService.getAllReceipts(organizationId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get receipt by ID")
    public ResponseEntity<ARReceipt> getReceiptById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARReceipt receipt = receiptService.getReceiptById(id);
        accountingRbac.requireAccountingView(actor, receipt.getOrganizationId());
        log.info("GET /api/ar/receipts/{}", id);
        return ResponseEntity.ok(receipt);
    }

    @PostMapping
    @Operation(summary = "Create new receipt")
    public ResponseEntity<ARReceipt> createReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ReceiptRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        log.info("POST /api/ar/receipts - Creating receipt: {}", request.getReceiptNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createReceipt(request));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post receipt (change status from DRAFT to POSTED)")
    public ResponseEntity<ARReceipt> postReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARReceipt existing = receiptService.getReceiptById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("POST /api/ar/receipts/{}/post", id);
        return ResponseEntity.ok(receiptService.postReceipt(id, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete receipt")
    public ResponseEntity<Void> deleteReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARReceipt existing = receiptService.getReceiptById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/ar/receipts/{}", id);
        receiptService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }
}
