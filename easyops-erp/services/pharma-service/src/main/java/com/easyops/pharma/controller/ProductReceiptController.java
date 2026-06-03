package com.easyops.pharma.controller;

import com.easyops.pharma.entity.ProductReceipt;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.ProductReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/product-receipts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Receipt", description = "Product receipt from factory to depot management APIs")
@CrossOrigin(origins = "*")
public class ProductReceiptController {

    private final ProductReceiptService receiptService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all product receipts")
    public ResponseEntity<List<ProductReceipt>> getAllReceipts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/product-receipts - organizationId: {}", organizationId);
        List<ProductReceipt> receipts = receiptService.getAllReceipts(organizationId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get receipts by date range")
    public ResponseEntity<List<ProductReceipt>> getReceiptsByDateRange(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/product-receipts/date-range - org: {}, dates: {} to {}", organizationId, startDate, endDate);
        List<ProductReceipt> receipts = receiptService.getReceiptsByDateRange(organizationId, startDate, endDate);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get receipt by ID")
    public ResponseEntity<ProductReceipt> getReceiptById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductReceipt receipt = receiptService.getReceiptById(id);
        pharmaRbac.requirePharmaView(actor, receipt.getOrganizationId());
        log.info("GET /api/pharma/product-receipts/{}", id);
        return ResponseEntity.ok(receipt);
    }

    @PostMapping
    @Operation(summary = "Create new product receipt")
    public ResponseEntity<ProductReceipt> createReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ProductReceipt receipt) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, receipt.getOrganizationId());
        log.info("POST /api/pharma/product-receipts");
        ProductReceipt created = receiptService.createReceipt(receipt);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product receipt")
    public ResponseEntity<ProductReceipt> updateReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProductReceipt receipt) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, receipt.getOrganizationId());
        log.info("PUT /api/pharma/product-receipts/{}", id);
        ProductReceipt updated = receiptService.updateReceipt(id, receipt);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit product receipt")
    public ResponseEntity<ProductReceipt> submitReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductReceipt existing = receiptService.getReceiptById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("POST /api/pharma/product-receipts/{}/submit", id);
        ProductReceipt receipt = receiptService.submitReceipt(id);
        return ResponseEntity.ok(receipt);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product receipt")
    public ResponseEntity<Void> deleteReceipt(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductReceipt existing = receiptService.getReceiptById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/product-receipts/{}", id);
        receiptService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }
}
