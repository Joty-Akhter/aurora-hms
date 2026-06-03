package com.easyops.hospitalbilling.domain.payment;

import com.easyops.hospitalbilling.api.dto.*;
import com.easyops.hospitalbilling.domain.invoice.Invoice;
import com.easyops.hospitalbilling.domain.invoice.InvoiceRepository;
import com.easyops.hospitalbilling.domain.invoice.InvoiceService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public PaymentResponse createPayment(UUID invoiceId, CreatePaymentRequest request, UUID receivedByUserId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (!"ISSUED".equals(invoice.getStatus())) {
            throw new IllegalStateException("Payments can only be recorded for ISSUED invoices");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setPaymentReference(request.getPaymentReference());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : OffsetDateTime.now());
        payment.setStatus("COMPLETED");
        payment.setReceivedByUserId(receivedByUserId);
        payment.setCreatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(payment.getCreatedAt());

        Payment saved = paymentRepository.save(payment);

        invoiceService.recomputeBalanceAndStatus(invoiceId);

        meterRegistry.counter("billing_payments_created_total").increment();

        return toPaymentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPaymentsForInvoice(UUID invoiceId) {
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        return payments.stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(OffsetDateTime from, OffsetDateTime to, UUID invoiceId) {
        List<Payment> payments;
        if (invoiceId != null) {
            payments = paymentRepository.findByInvoiceId(invoiceId);
        } else if (from != null && to != null) {
            payments = paymentRepository.findByPaymentDateBetween(from, to);
        } else {
            payments = paymentRepository.findAll();
        }
        return payments.stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        List<Refund> refunds = refundRepository.findByOriginalPaymentId(paymentId);

        PaymentDetailResponse detail = new PaymentDetailResponse();
        detail.setPayment(toPaymentResponse(payment));
        detail.setRefunds(refunds.stream().map(this::toRefundResponse).collect(Collectors.toList()));
        return detail;
    }

    @Transactional
    public RefundResponse createRefund(UUID paymentId, CreateRefundRequest request, UUID processedByUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        BigDecimal alreadyRefunded = refundRepository.findByOriginalPaymentId(paymentId).stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds remaining refundable amount");
        }

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setOriginalPaymentId(paymentId);
        refund.setInvoiceId(payment.getInvoiceId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        OffsetDateTime now = OffsetDateTime.now();
        refund.setProcessedAt(now);
        refund.setProcessedByUserId(processedByUserId);
        refund.setCreatedAt(now);

        Refund saved = refundRepository.save(refund);

        invoiceService.recomputeBalanceAndStatus(payment.getInvoiceId());

        meterRegistry.counter("billing_refunds_created_total").increment();

        return toRefundResponse(saved);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        PaymentResponse dto = new PaymentResponse();
        dto.setId(payment.getId());
        dto.setInvoiceId(payment.getInvoiceId());
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setStatus(payment.getStatus());
        dto.setReceivedByUserId(payment.getReceivedByUserId());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private RefundResponse toRefundResponse(Refund refund) {
        RefundResponse dto = new RefundResponse();
        dto.setId(refund.getId());
        dto.setOriginalPaymentId(refund.getOriginalPaymentId());
        dto.setInvoiceId(refund.getInvoiceId());
        dto.setAmount(refund.getAmount());
        dto.setReason(refund.getReason());
        dto.setProcessedAt(refund.getProcessedAt());
        dto.setProcessedByUserId(refund.getProcessedByUserId());
        return dto;
    }
}

