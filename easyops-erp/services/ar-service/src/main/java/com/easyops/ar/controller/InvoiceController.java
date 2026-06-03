package com.easyops.ar.controller;

import com.easyops.ar.dto.InvoiceRequest;
import com.easyops.ar.entity.ARInvoice;
import com.easyops.ar.security.AccountingRbacService;
import com.easyops.ar.security.RbacRequestHeaders;
import com.easyops.ar.service.InvoiceService;
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
@RequestMapping("/api/ar/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AR Invoices", description = "Invoice management for Accounts Receivable")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AccountingRbacService accountingRbac;

    @GetMapping
    @Operation(summary = "Get all invoices for an organization")
    public ResponseEntity<List<ARInvoice>> getAllInvoices(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/invoices - organizationId: {}, status: {}", organizationId, status);
        List<ARInvoice> invoices = status != null
                ? invoiceService.getInvoicesByStatus(organizationId, status)
                : invoiceService.getAllInvoices(organizationId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/outstanding")
    @Operation(summary = "Get outstanding invoices (unpaid)")
    public ResponseEntity<List<ARInvoice>> getOutstandingInvoices(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/invoices/outstanding - organizationId: {}", organizationId);
        return ResponseEntity.ok(invoiceService.getOutstandingInvoices(organizationId));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue invoices")
    public ResponseEntity<List<ARInvoice>> getOverdueInvoices(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        log.info("GET /api/ar/invoices/overdue - organizationId: {}", organizationId);
        return ResponseEntity.ok(invoiceService.getOverdueInvoices(organizationId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ARInvoice> getInvoiceById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARInvoice invoice = invoiceService.getInvoiceById(id);
        accountingRbac.requireAccountingView(actor, invoice.getOrganizationId());
        log.info("GET /api/ar/invoices/{}", id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping
    @Operation(summary = "Create new invoice")
    public ResponseEntity<ARInvoice> createInvoice(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody InvoiceRequest request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, request.getOrganizationId());
        log.info("POST /api/ar/invoices - Creating invoice: {}", request.getInvoiceNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request, actor));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post invoice (change status from DRAFT to POSTED)")
    public ResponseEntity<ARInvoice> postInvoice(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARInvoice existing = invoiceService.getInvoiceById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("POST /api/ar/invoices/{}/post", id);
        return ResponseEntity.ok(invoiceService.postInvoice(id, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete invoice")
    public ResponseEntity<Void> deleteInvoice(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ARInvoice existing = invoiceService.getInvoiceById(id);
        accountingRbac.requireAccountingManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/ar/invoices/{}", id);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
