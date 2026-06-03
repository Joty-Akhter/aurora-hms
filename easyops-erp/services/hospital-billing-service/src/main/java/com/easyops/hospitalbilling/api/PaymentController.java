package com.easyops.hospitalbilling.api;

import com.easyops.hospitalbilling.api.dto.*;
import com.easyops.hospitalbilling.domain.payment.PaymentService;
import com.easyops.hospitalbilling.security.HospitalBillingRbacService;
import com.easyops.hospitalbilling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-billing")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final HospitalBillingRbacService hospitalBillingRbac;

    @PostMapping("/invoices/{id}/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable("id") UUID invoiceId,
            @RequestBody CreatePaymentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        PaymentResponse created = paymentService.createPayment(invoiceId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/invoices/{id}/payments")
    public List<PaymentResponse> listPayments(
            @PathVariable("id") UUID invoiceId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return paymentService.listPaymentsForInvoice(invoiceId);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> listPaymentsGlobal(
            @RequestParam(value = "invoiceId", required = false) UUID invoiceId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return paymentService.listPayments(from, to, invoiceId);
    }

    @GetMapping("/payments/{id}")
    public PaymentDetailResponse getPayment(
            @PathVariable("id") UUID paymentId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return paymentService.getPayment(paymentId);
    }

    @PostMapping("/payments/{id}/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable("id") UUID paymentId,
            @RequestBody CreateRefundRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalManage(actor, organizationId);
        RefundResponse created = paymentService.createRefund(paymentId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
