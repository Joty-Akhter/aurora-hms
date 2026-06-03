package com.easyops.hospitalbilling.domain.adjustment;

import com.easyops.hospitalbilling.api.dto.CreateAdjustmentRequest;
import com.easyops.hospitalbilling.api.dto.InvoiceDetailResponse;
import com.easyops.hospitalbilling.domain.invoice.Invoice;
import com.easyops.hospitalbilling.domain.invoice.InvoiceRepository;
import com.easyops.hospitalbilling.domain.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdjustmentService {

    private final AdjustmentRepository adjustmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public InvoiceDetailResponse createAdjustment(UUID invoiceId, CreateAdjustmentRequest request, UUID approvedByUserId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (!"ISSUED".equals(invoice.getStatus()) && !"PARTIALLY_PAID".equals(invoice.getStatus())) {
            throw new IllegalStateException("Adjustments can only be recorded for ISSUED or PARTIALLY_PAID invoices");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Adjustment amount must be positive");
        }

        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        String normalizedType = request.getType().toUpperCase(Locale.ROOT);
        if (!normalizedType.equals("WRITE_OFF") && !normalizedType.equals("CREDIT") && !normalizedType.equals("ADJUSTMENT")) {
            throw new IllegalArgumentException("Invalid adjustment type: " + request.getType());
        }

        Adjustment adjustment = new Adjustment();
        adjustment.setId(UUID.randomUUID());
        adjustment.setInvoiceId(invoiceId);
        adjustment.setType(normalizedType);
        adjustment.setAmount(request.getAmount());
        adjustment.setReason(request.getReason());
        adjustment.setApprovedByUserId(approvedByUserId);
        adjustment.setCreatedAt(OffsetDateTime.now());

        adjustmentRepository.save(adjustment);

        invoiceService.recomputeBalanceAndStatus(invoiceId);

        return invoiceService.getInvoice(invoiceId);
    }
}

