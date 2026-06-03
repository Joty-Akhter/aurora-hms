package com.easyops.hospitalbilling.api;

import com.easyops.hospitalbilling.api.dto.CreateAdjustmentRequest;
import com.easyops.hospitalbilling.api.dto.CreateInvoiceDiscountLineRequest;
import com.easyops.hospitalbilling.api.dto.CreateInvoiceRequest;
import com.easyops.hospitalbilling.api.dto.DiscountLineResponse;
import com.easyops.hospitalbilling.api.dto.EstimateRequest;
import com.easyops.hospitalbilling.api.dto.EstimateResponse;
import com.easyops.hospitalbilling.api.dto.InvoiceDetailResponse;
import com.easyops.hospitalbilling.api.dto.InvoiceResponse;
import com.easyops.hospitalbilling.api.dto.PagedResponse;
import com.easyops.hospitalbilling.domain.adjustment.AdjustmentService;
import com.easyops.hospitalbilling.domain.invoice.InvoiceService;
import com.easyops.hospitalbilling.security.HospitalBillingRbacService;
import com.easyops.hospitalbilling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AdjustmentService adjustmentService;
    private final HospitalBillingRbacService hospitalBillingRbac;

    @PostMapping
    public ResponseEntity<InvoiceDetailResponse> createInvoice(
            @RequestBody CreateInvoiceRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        InvoiceDetailResponse created = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/estimate")
    public EstimateResponse computeEstimate(
            @RequestBody EstimateRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return invoiceService.computeEstimate(request);
    }

    @GetMapping("/{id}/discounts")
    public List<DiscountLineResponse> getInvoiceDiscounts(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return invoiceService.getAppliedDiscounts(id);
    }

    @PostMapping("/{id}/discount-lines")
    public ResponseEntity<DiscountLineResponse> addInvoiceDiscountLine(
            @PathVariable("id") UUID id,
            @RequestBody CreateInvoiceDiscountLineRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        DiscountLineResponse created = invoiceService.addInvoiceDiscountLine(
                id,
                request.getDescription(),
                request.getSource(),
                request.getAmount(),
                request.getCreatedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<InvoiceDetailResponse> createAdjustment(
            @PathVariable("id") UUID id,
            @RequestBody CreateAdjustmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        InvoiceDetailResponse updated = adjustmentService.createAdjustment(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @GetMapping("/{id}")
    public InvoiceDetailResponse getInvoice(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return invoiceService.getInvoice(id);
    }

    @GetMapping
    public PagedResponse<InvoiceResponse> listInvoices(
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "visitId", required = false) UUID visitId,
            @RequestParam(value = "status", required = false) java.util.List<String> statuses,
            @RequestParam(value = "payerType", required = false) String payerType,
            @RequestParam(value = "issuedFrom", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime issuedFrom,
            @RequestParam(value = "issuedTo", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime issuedTo,
            @RequestParam(value = "createdFrom", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime createdFrom,
            @RequestParam(value = "createdTo", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime createdTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return invoiceService.listInvoices(
                patientId,
                visitId,
                statuses,
                payerType,
                issuedFrom,
                issuedTo,
                createdFrom,
                createdTo,
                page,
                size
        );
    }

    @PostMapping("/{id}/issue")
    public InvoiceResponse issueInvoice(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        return invoiceService.issueInvoice(id);
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancelInvoice(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        return invoiceService.cancelInvoice(id);
    }
}
