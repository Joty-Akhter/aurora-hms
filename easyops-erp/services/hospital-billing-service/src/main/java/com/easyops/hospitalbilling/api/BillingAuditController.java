package com.easyops.hospitalbilling.api;

import com.easyops.hospitalbilling.api.dto.BillingAuditEvent;
import com.easyops.hospitalbilling.domain.adjustment.Adjustment;
import com.easyops.hospitalbilling.domain.adjustment.AdjustmentRepository;
import com.easyops.hospitalbilling.domain.invoice.DiscountAuditLog;
import com.easyops.hospitalbilling.domain.invoice.DiscountAuditLogRepository;
import com.easyops.hospitalbilling.domain.payment.Payment;
import com.easyops.hospitalbilling.domain.payment.PaymentRepository;
import com.easyops.hospitalbilling.domain.payment.Refund;
import com.easyops.hospitalbilling.domain.payment.RefundRepository;
import com.easyops.hospitalbilling.security.HospitalBillingRbacService;
import com.easyops.hospitalbilling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-billing/audit")
@RequiredArgsConstructor
public class BillingAuditController {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final DiscountAuditLogRepository discountAuditLogRepository;
    private final HospitalBillingRbacService hospitalBillingRbac;

    @GetMapping("/invoices/{invoiceId}")
    public List<BillingAuditEvent> getInvoiceAudit(
            @PathVariable("invoiceId") UUID invoiceId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        List<BillingAuditEvent> events = new ArrayList<>();

        for (Payment payment : paymentRepository.findByInvoiceId(invoiceId)) {
            BillingAuditEvent e = new BillingAuditEvent();
            e.setType("PAYMENT");
            e.setInvoiceId(invoiceId);
            e.setPaymentId(payment.getId());
            e.setAmount(payment.getAmount());
            e.setUserId(payment.getReceivedByUserId());
            e.setOccurredAt(payment.getPaymentDate());
            e.setDescription("Payment recorded");
            e.setSource(payment.getPaymentMethod());
            events.add(e);
        }

        for (Refund refund : refundRepository.findByInvoiceId(invoiceId)) {
            BillingAuditEvent e = new BillingAuditEvent();
            e.setType("REFUND");
            e.setInvoiceId(invoiceId);
            e.setRefundId(refund.getId());
            e.setPaymentId(refund.getOriginalPaymentId());
            e.setAmount(refund.getAmount());
            e.setUserId(refund.getProcessedByUserId());
            e.setOccurredAt(refund.getProcessedAt());
            e.setDescription("Refund processed");
            events.add(e);
        }

        for (Adjustment adj : adjustmentRepository.findByInvoiceId(invoiceId)) {
            BillingAuditEvent e = new BillingAuditEvent();
            e.setType("ADJUSTMENT");
            e.setInvoiceId(invoiceId);
            e.setAdjustmentId(adj.getId());
            e.setAmount(adj.getAmount());
            e.setUserId(adj.getApprovedByUserId());
            e.setOccurredAt(adj.getCreatedAt());
            e.setDescription(adj.getType());
            events.add(e);
        }

        for (DiscountAuditLog log : discountAuditLogRepository.findByInvoiceId(invoiceId)) {
            BillingAuditEvent e = new BillingAuditEvent();
            e.setType("DISCOUNT");
            e.setInvoiceId(invoiceId);
            e.setChargeLineId(log.getChargeLineId());
            e.setAmount(log.getDiscountAmount());
            e.setUserId(log.getAppliedByUserId());
            e.setOccurredAt(log.getAppliedAt());
            e.setDescription("Discount applied");
            e.setSource(log.getSource());
            events.add(e);
        }

        events.sort(Comparator.comparing(
                BillingAuditEvent::getOccurredAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return events;
    }
}
