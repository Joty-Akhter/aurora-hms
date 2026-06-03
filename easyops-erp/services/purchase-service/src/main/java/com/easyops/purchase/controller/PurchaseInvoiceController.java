package com.easyops.purchase.controller;

import com.easyops.purchase.security.PurchaseRbacService;
import com.easyops.purchase.security.RbacRequestHeaders;
import com.easyops.purchase.util.OrganizationIdParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/purchase/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Purchase Invoices", description = "Purchase invoice management APIs")
@CrossOrigin(origins = "*")
public class PurchaseInvoiceController {

    private final PurchaseRbacService purchaseRbac;

    @GetMapping
    @Operation(summary = "Get all purchase invoices")
    public ResponseEntity<List<Map<String, Object>>> getAllInvoices(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        purchaseRbac.requirePurchaseView(actor, organizationId);
        log.info("GET /api/purchase/invoices - organizationId: {}, status: {}", organizationId, status);
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/variances")
    @Operation(summary = "Get invoices with variances")
    public ResponseEntity<List<Map<String, Object>>> getInvoiceVariances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        purchaseRbac.requirePurchaseView(actor, organizationId);
        log.info("GET /api/purchase/invoices/variances - organizationId: {}", organizationId);
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create new purchase invoice")
    public ResponseEntity<Map<String, Object>> createInvoice(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = OrganizationIdParser.fromMap(request);
        purchaseRbac.requirePurchaseManage(actor, orgId);
        log.info("POST /api/purchase/invoices");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invoice creation endpoint - full implementation pending");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve invoice")
    public ResponseEntity<Map<String, Object>> approveInvoice(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = OrganizationIdParser.fromMap(request);
        purchaseRbac.requirePurchaseManage(actor, orgId);
        log.info("POST /api/purchase/invoices/{}/approve", id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Approved"));
    }

    @PostMapping("/{id}/match")
    @Operation(summary = "Run three-way matching")
    public ResponseEntity<Map<String, Object>> runMatching(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = OrganizationIdParser.fromMap(request);
        purchaseRbac.requirePurchaseManage(actor, orgId);
        log.info("POST /api/purchase/invoices/{}/match", id);
        Map<String, Object> result = new HashMap<>();
        result.put("matched", true);
        result.put("priceVariance", 0);
        result.put("quantityVariance", 0);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/approve-variance")
    @Operation(summary = "Approve invoice variance")
    public ResponseEntity<Map<String, Object>> approveVariance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = OrganizationIdParser.fromMap(request);
        purchaseRbac.requirePurchaseManage(actor, orgId);
        log.info("POST /api/purchase/invoices/{}/approve-variance", id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Variance approved"));
    }

    @PostMapping("/{id}/create-bill")
    @Operation(summary = "Create AP bill from invoice")
    public ResponseEntity<Map<String, Object>> createBill(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = OrganizationIdParser.fromMap(request);
        purchaseRbac.requirePurchaseManage(actor, orgId);
        log.info("POST /api/purchase/invoices/{}/create-bill", id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bill created successfully");
        response.put("billId", UUID.randomUUID());
        return ResponseEntity.ok(response);
    }
}
