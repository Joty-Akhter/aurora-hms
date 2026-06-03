package com.easyops.hospitalbilling.api;

import com.easyops.hospitalbilling.api.dto.CollectedPaymentReportItem;
import com.easyops.hospitalbilling.api.dto.OutstandingInvoiceReportItem;
import com.easyops.hospitalbilling.domain.invoice.Invoice;
import com.easyops.hospitalbilling.domain.invoice.InvoiceRepository;
import com.easyops.hospitalbilling.domain.invoice.InvoiceSpecifications;
import com.easyops.hospitalbilling.domain.payment.Payment;
import com.easyops.hospitalbilling.domain.payment.PaymentRepository;
import com.easyops.hospitalbilling.security.HospitalBillingRbacService;
import com.easyops.hospitalbilling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hospital-billing/reports")
@RequiredArgsConstructor
public class BillingReportController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final HospitalBillingRbacService hospitalBillingRbac;

    @GetMapping("/outstanding")
    public List<OutstandingInvoiceReportItem> getOutstandingInvoices(
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "corporateId", required = false) UUID corporateId,
            @RequestParam(value = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime asOf,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        var spec = InvoiceSpecifications.hasPatientId(patientId)
                .and(corporateId == null
                        ? null
                        : (root, query, cb) -> cb.equal(root.get("payerId"), corporateId))
                .and((root, query, cb) -> cb.greaterThan(root.get("balanceDue"), BigDecimal.ZERO));

        if (asOf != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), asOf));
        }

        List<Invoice> invoices = invoiceRepository.findAll(spec);

        return invoices.stream().map(this::toOutstandingItem).collect(Collectors.toList());
    }

    @GetMapping("/collected")
    public List<CollectedPaymentReportItem> getCollectedPayments(
            @RequestParam(value = "from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(from, to);
        return payments.stream().map(this::toCollectedItem).collect(Collectors.toList());
    }

    private OutstandingInvoiceReportItem toOutstandingItem(Invoice invoice) {
        OutstandingInvoiceReportItem item = new OutstandingInvoiceReportItem();
        item.setInvoiceId(invoice.getId());
        item.setInvoiceNumber(invoice.getInvoiceNumber());
        item.setPatientId(invoice.getPatientId());
        item.setPayerId(invoice.getPayerId());
        item.setPayerType(invoice.getPayerType());
        item.setNetAmount(invoice.getNetAmount());
        item.setBalanceDue(invoice.getBalanceDue());
        item.setIssuedAt(invoice.getIssuedAt());
        return item;
    }

    private CollectedPaymentReportItem toCollectedItem(Payment payment) {
        CollectedPaymentReportItem item = new CollectedPaymentReportItem();
        item.setPaymentId(payment.getId());
        item.setInvoiceId(payment.getInvoiceId());
        item.setAmount(payment.getAmount());
        item.setPaymentMethod(payment.getPaymentMethod());
        item.setPaymentDate(payment.getPaymentDate());
        item.setReceivedByUserId(payment.getReceivedByUserId());
        // Invoice number optional: resolved lazily to avoid extra join in simple report
        return item;
    }
}
