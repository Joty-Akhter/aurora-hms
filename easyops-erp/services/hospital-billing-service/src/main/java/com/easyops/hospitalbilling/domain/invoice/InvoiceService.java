package com.easyops.hospitalbilling.domain.invoice;

import com.easyops.hospitalbilling.api.dto.*;
import com.easyops.hospitalbilling.domain.adjustment.Adjustment;
import com.easyops.hospitalbilling.domain.adjustment.AdjustmentRepository;
import com.easyops.hospitalbilling.domain.charge.ChargeLine;
import com.easyops.hospitalbilling.integration.DiscountRulesClient;
import com.easyops.hospitalbilling.integration.CorporateCardValidationClient;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalbilling.integration.dto.LineItemForDiscountRequest;
import com.easyops.hospitalbilling.integration.dto.LineDiscountResult;
import com.easyops.hospitalbilling.domain.charge.ChargeLineRepository;
import com.easyops.hospitalbilling.domain.charge.ChargeSpecifications;
import com.easyops.hospitalbilling.domain.payment.Payment;
import com.easyops.hospitalbilling.domain.payment.PaymentRepository;
import com.easyops.hospitalbilling.domain.payment.Refund;
import com.easyops.hospitalbilling.domain.payment.RefundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ChargeLineRepository chargeLineRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final InvoiceDiscountLineRepository invoiceDiscountLineRepository;
    private final DiscountAuditLogRepository discountAuditLogRepository;
    private final DiscountRulesClient discountRulesClient;
    private final CorporateCardValidationClient corporateCardValidationClient;
    private final MeterRegistry meterRegistry;
    private final CommunicationInvoiceEventPublisher communicationInvoiceEventPublisher;
    private final LegacyInvoiceNotificationFallbackService legacyInvoiceNotificationFallbackService;
    @Value("${comm.invoice.email.enabled:false}")
    private boolean commInvoiceEmailEnabled;

    @Transactional
    public InvoiceDetailResponse createInvoice(CreateInvoiceRequest request) {
        List<ChargeLine> chargeLines;
        if (request.getChargeLineIds() != null && !request.getChargeLineIds().isEmpty()) {
            chargeLines = chargeLineRepository.findAllById(request.getChargeLineIds());
            if (chargeLines.size() != request.getChargeLineIds().size()) {
                throw new IllegalArgumentException("One or more charge lines not found");
            }
        } else if (request.getGroupBy() != null && !request.getGroupBy().isBlank()) {
            if (request.getPatientId() == null) {
                throw new IllegalArgumentException("patientId is required when using groupBy");
            }
            List<String> statuses = Collections.singletonList("PENDING");
            Specification<ChargeLine> spec = Specification
                    .where(ChargeSpecifications.hasPatientId(request.getPatientId()))
                    .and("VISIT".equalsIgnoreCase(request.getGroupBy())
                            ? ChargeSpecifications.hasVisitId(request.getVisitId())
                            : null)
                    .and(ChargeSpecifications.hasStatuses(statuses));
            chargeLines = chargeLineRepository.findAll(spec);
            if (chargeLines.isEmpty()) {
                throw new IllegalArgumentException("No pending charges found for grouping criteria");
            }
        } else {
            throw new IllegalArgumentException("Either chargeLineIds or groupBy must be provided");
        }

        UUID patientId = chargeLines.get(0).getPatientId();
        boolean samePatient = chargeLines.stream()
                .allMatch(cl -> Objects.equals(cl.getPatientId(), patientId));
        if (!samePatient) {
            throw new IllegalArgumentException("All charge lines must belong to the same patient");
        }

        boolean allInAllowedStatus = chargeLines.stream()
                .allMatch(cl -> "PENDING".equals(cl.getStatus()) || "POSTED".equals(cl.getStatus()));
        if (!allInAllowedStatus) {
            throw new IllegalArgumentException("All charge lines must be in PENDING or POSTED status");
        }

        BigDecimal gross = chargeLines.stream()
                .map(ChargeLine::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = chargeLines.stream()
                .map(cl -> Optional.ofNullable(cl.getDiscountAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = chargeLines.stream()
                .map(cl -> Optional.ofNullable(cl.getTaxAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = chargeLines.stream()
                .map(ChargeLine::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setPatientId(patientId);
        invoice.setVisitId(request.getVisitId());
        invoice.setPayerType(request.getPayerType());
        invoice.setPayerId(request.getPayerId());
        invoice.setStatus("DRAFT");
        invoice.setGrossAmount(gross);
        invoice.setTotalDiscount(discount);
        invoice.setTaxAmount(tax);
        invoice.setNetAmount(net);
        invoice.setBalanceDue(net);
        invoice.setDueDate(request.getDueDate());
        OffsetDateTime now = OffsetDateTime.now();
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        meterRegistry.counter("billing_invoices_created_total").increment();

        // Link charge lines to invoice and mark as POSTED
        for (ChargeLine cl : chargeLines) {
            cl.setInvoiceId(savedInvoice.getId());
            cl.setStatus("POSTED");
        }
        chargeLineRepository.saveAll(chargeLines);

        // Discount audit: log each charge line that has a discount applied
        for (ChargeLine cl : chargeLines) {
            BigDecimal lineDiscount = Optional.ofNullable(cl.getDiscountAmount()).orElse(BigDecimal.ZERO);
            if (lineDiscount.compareTo(BigDecimal.ZERO) > 0) {
                DiscountAuditLog logEntry = new DiscountAuditLog();
                logEntry.setId(UUID.randomUUID());
                logEntry.setInvoiceId(savedInvoice.getId());
                logEntry.setChargeLineId(cl.getId());
                logEntry.setDiscountAmount(lineDiscount);
                logEntry.setSource(Optional.ofNullable(cl.getDiscountSource()).orElse("LINE"));
                logEntry.setCorporateContractId(cl.getCorporateContractId());
                logEntry.setAppliedAt(OffsetDateTime.now());
                discountAuditLogRepository.save(logEntry);
            }
        }

        if (commInvoiceEmailEnabled) {
            communicationInvoiceEventPublisher.publishInvoiceCreated(savedInvoice);
        } else {
            legacyInvoiceNotificationFallbackService.notifyInvoiceCreated(savedInvoice);
        }

        return buildDetailResponse(savedInvoice, chargeLines);
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse getInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        List<ChargeLine> lines = chargeLineRepository.findByInvoiceId(id);
        return buildDetailResponse(invoice, lines);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> listInvoices(
            UUID patientId,
            UUID visitId,
            List<String> statuses,
            String payerType,
            OffsetDateTime issuedFrom,
            OffsetDateTime issuedTo,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Specification<Invoice> spec = Specification
                .where(InvoiceSpecifications.hasPatientId(patientId))
                .and(InvoiceSpecifications.hasVisitId(visitId))
                .and(InvoiceSpecifications.hasStatuses(statuses))
                .and(InvoiceSpecifications.hasPayerType(payerType))
                .and(InvoiceSpecifications.issuedAtBetween(issuedFrom, issuedTo))
                .and(InvoiceSpecifications.createdAtBetween(createdFrom, createdTo));

        Page<Invoice> pageResult = invoiceRepository.findAll(spec, pageRequest);
        PagedResponse<InvoiceResponse> response = new PagedResponse<>();
        response.setContent(pageResult.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        response.setTotalElements(pageResult.getTotalElements());
        response.setTotalPages(pageResult.getTotalPages());
        response.setPage(pageResult.getNumber());
        response.setSize(pageResult.getSize());
        return response;
    }

    @Transactional
    public InvoiceResponse issueInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only DRAFT invoices can be issued");
        }
        invoice.setStatus("ISSUED");
        invoice.setIssuedAt(OffsetDateTime.now());
        invoice.setUpdatedAt(OffsetDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);
        meterRegistry.counter("billing_invoices_issued_total").increment();
        return toDto(saved);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        invoice.setStatus("CANCELLED");
        invoice.setUpdatedAt(OffsetDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);
        return toDto(saved);
    }

    private InvoiceDetailResponse buildDetailResponse(Invoice invoice, List<ChargeLine> lines) {
        InvoiceDetailResponse detail = new InvoiceDetailResponse();
        detail.setInvoice(toDto(invoice));
        List<ChargeResponse> lineDtos = lines.stream().map(this::toChargeDto).collect(Collectors.toList());
        detail.setChargeLines(lineDtos);

        PaymentsSummary summary = buildPaymentsSummary(invoice.getId());
        detail.setPaymentsSummary(summary);

        return detail;
    }

    private InvoiceResponse toDto(Invoice invoice) {
        InvoiceResponse dto = new InvoiceResponse();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setPatientId(invoice.getPatientId());
        dto.setVisitId(invoice.getVisitId());
        dto.setPayerType(invoice.getPayerType());
        dto.setPayerId(invoice.getPayerId());
        dto.setStatus(invoice.getStatus());
        dto.setGrossAmount(invoice.getGrossAmount());
        dto.setTotalDiscount(invoice.getTotalDiscount());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setBalanceDue(invoice.getBalanceDue());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setDueDate(invoice.getDueDate());
        return dto;
    }

    private ChargeResponse toChargeDto(ChargeLine e) {
        ChargeResponse dto = new ChargeResponse();
        dto.setId(e.getId());
        dto.setSourceService(e.getSourceService());
        dto.setSourceReferenceId(e.getSourceReferenceId());
        dto.setPatientId(e.getPatientId());
        dto.setVisitId(e.getVisitId());
        dto.setCorporateContractId(e.getCorporateContractId());
        dto.setItemCode(e.getItemCode());
        dto.setItemDescription(e.getItemDescription());
        dto.setQuantity(e.getQuantity());
        dto.setUnitPrice(e.getUnitPrice());
        dto.setGrossAmount(e.getGrossAmount());
        dto.setDiscountAmount(e.getDiscountAmount());
        dto.setDiscountSource(e.getDiscountSource());
        dto.setTaxAmount(e.getTaxAmount());
        dto.setNetAmount(e.getNetAmount());
        dto.setStatus(e.getStatus());
        dto.setInvoiceId(e.getInvoiceId());
        return dto;
    }

    private String generateInvoiceNumber() {
        // Simple placeholder: use UUID suffix; can be replaced with sequence-based scheme later.
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "HB-" + uuidPart;
    }

    @Transactional
    public void recomputeBalanceAndStatus(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        PaymentsSummary summary = buildPaymentsSummary(invoiceId);
        BigDecimal totalPaid = Optional.ofNullable(summary.getTotalPaid()).orElse(BigDecimal.ZERO);

        BigDecimal netAmount = Optional.ofNullable(invoice.getNetAmount()).orElse(BigDecimal.ZERO);

        BigDecimal totalAdjustments = adjustmentRepository.findByInvoiceId(invoiceId).stream()
                .map(Adjustment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal effectiveNetAmount = netAmount.subtract(totalAdjustments);
        if (effectiveNetAmount.compareTo(BigDecimal.ZERO) < 0) {
            effectiveNetAmount = BigDecimal.ZERO;
        }

        BigDecimal newBalance = effectiveNetAmount.subtract(totalPaid);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        invoice.setBalanceDue(newBalance);

        OffsetDateTime now = OffsetDateTime.now();

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            // No net payments: revert to ISSUED if it was previously paid/partially paid
            if ("PAID".equals(invoice.getStatus()) || "PARTIALLY_PAID".equals(invoice.getStatus())) {
                invoice.setStatus("ISSUED");
            }
        } else if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus("PAID");
        } else {
            invoice.setStatus("PARTIALLY_PAID");
        }

        invoice.setUpdatedAt(now);
        invoiceRepository.save(invoice);
    }

    /**
     * Computes an estimate for the given line items. Uses {@link DiscountRulesClient} to apply
     * discounts when hospital-corporate-and-discount-service is available and enabled; otherwise
     * the stub returns zero discount.
     */
    @Transactional(readOnly = true)
    public EstimateResponse computeEstimate(EstimateRequest request) {
        if (request == null || request.getLineItems() == null || request.getLineItems().isEmpty()) {
            EstimateResponse empty = new EstimateResponse();
            empty.setLines(Collections.emptyList());
            empty.setDiscountLines(Collections.emptyList());
            empty.setTotalGross(BigDecimal.ZERO);
            empty.setTotalDiscount(BigDecimal.ZERO);
            empty.setNetPayable(BigDecimal.ZERO);
            return empty;
        }

        if (request.getCardNumber() != null && !request.getCardNumber().isBlank()) {
            var validation = corporateCardValidationClient.validateCard(request.getCardNumber().trim());
            if (validation == null || !validation.isValid()) {
                String message = validation != null && validation.getMessage() != null
                        ? validation.getMessage()
                        : "Corporate card is not valid";
                throw new IllegalArgumentException("Invalid corporate card: " + message);
            }
            if (request.getCorporateContractId() == null && validation.getContractId() != null) {
                request.setCorporateContractId(validation.getContractId());
            }
            if (request.getCorporateClientId() == null && validation.getCorporateClientId() != null) {
                request.setCorporateClientId(validation.getCorporateClientId());
            }
        }

        List<LineItemForDiscountRequest> discountRequestItems = new ArrayList<>();
        List<EstimateLineItemRequest> items = request.getLineItems();
        for (EstimateLineItemRequest item : items) {
            BigDecimal qty = Optional.ofNullable(item.getQuantity()).orElse(BigDecimal.ONE);
            BigDecimal unitPrice = Optional.ofNullable(item.getUnitPrice()).orElse(BigDecimal.ZERO);
            LineItemForDiscountRequest di = new LineItemForDiscountRequest();
            di.setServiceCode(item.getItemCode());
            di.setQuantity(qty);
            di.setUnitPrice(unitPrice);
            discountRequestItems.add(di);
        }

        EvaluateDiscountsRequest evalRequest = new EvaluateDiscountsRequest();
        evalRequest.setPatientId(request.getPatientId());
        evalRequest.setCorporateClientId(request.getCorporateClientId());
        evalRequest.setItems(discountRequestItems);
        var discountResponse = discountRulesClient.evaluateDiscounts(evalRequest);

        Map<Integer, LineDiscountResult> discountByIndex = new HashMap<>();
        if (discountResponse.getLineDiscounts() != null) {
            for (LineDiscountResult r : discountResponse.getLineDiscounts()) {
                discountByIndex.put(r.getLineIndex(), r);
            }
        }
        if (discountByIndex.isEmpty()
                && discountResponse.getRecommendedTotalDiscount() != null
                && discountResponse.getRecommendedTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fallbackGross = items.stream()
                    .map(i -> Optional.ofNullable(i.getQuantity()).orElse(BigDecimal.ONE)
                            .multiply(Optional.ofNullable(i.getUnitPrice()).orElse(BigDecimal.ZERO)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (fallbackGross.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal allocated = BigDecimal.ZERO;
                for (int i = 0; i < items.size(); i++) {
                    BigDecimal gross = Optional.ofNullable(items.get(i).getQuantity()).orElse(BigDecimal.ONE)
                            .multiply(Optional.ofNullable(items.get(i).getUnitPrice()).orElse(BigDecimal.ZERO));
                    BigDecimal share = (i == items.size() - 1)
                            ? discountResponse.getRecommendedTotalDiscount().subtract(allocated)
                            : discountResponse.getRecommendedTotalDiscount().multiply(gross)
                                    .divide(fallbackGross, 4, RoundingMode.HALF_UP);
                    allocated = allocated.add(share);
                    LineDiscountResult r = new LineDiscountResult();
                    r.setLineIndex(i);
                    r.setDiscountAmount(share.max(BigDecimal.ZERO));
                    r.setSource("CORPORATE_SCHEME");
                    discountByIndex.put(i, r);
                }
            }
        }

        List<EstimateLineResponse> lines = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<DiscountLineResponse> discountLines = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            EstimateLineItemRequest item = items.get(i);
            BigDecimal qty = Optional.ofNullable(item.getQuantity()).orElse(BigDecimal.ONE);
            BigDecimal unitPrice = Optional.ofNullable(item.getUnitPrice()).orElse(BigDecimal.ZERO);
            BigDecimal gross = qty.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP);
            LineDiscountResult lineDiscount = discountByIndex.get(i);
            BigDecimal discount = (lineDiscount != null && lineDiscount.getDiscountAmount() != null)
                ? lineDiscount.getDiscountAmount().min(gross).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal net = gross.subtract(discount).setScale(4, RoundingMode.HALF_UP);

            EstimateLineResponse lineResp = new EstimateLineResponse();
            lineResp.setItemCode(item.getItemCode());
            lineResp.setItemDescription(item.getItemDescription());
            lineResp.setQuantity(qty);
            lineResp.setUnitPrice(unitPrice);
            lineResp.setGrossAmount(gross);
            lineResp.setDiscountAmount(discount);
            lineResp.setNetAmount(net);
            lines.add(lineResp);

            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                DiscountLineResponse dl = new DiscountLineResponse();
                dl.setDescription(Optional.ofNullable(item.getItemDescription()).filter(s -> s != null && !s.isBlank()).orElse(item.getItemCode()));
                dl.setSource(lineDiscount != null && lineDiscount.getSource() != null ? lineDiscount.getSource() : "LINE");
                dl.setAmount(discount);
                discountLines.add(dl);
            }

            totalGross = totalGross.add(gross);
            totalDiscount = totalDiscount.add(discount);
        }

        EstimateResponse response = new EstimateResponse();
        response.setLines(lines);
        response.setDiscountLines(discountLines);
        response.setTotalGross(totalGross.setScale(4, RoundingMode.HALF_UP));
        response.setTotalDiscount(totalDiscount.setScale(4, RoundingMode.HALF_UP));
        response.setNetPayable(totalGross.subtract(totalDiscount).setScale(4, RoundingMode.HALF_UP));
        return response;
    }

    /**
     * Returns the list of applied discount lines for an invoice (from charge lines or invoice-level discounts).
     */
    @Transactional(readOnly = true)
    public List<DiscountLineResponse> getAppliedDiscounts(UUID invoiceId) {
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        List<ChargeLine> chargeLines = chargeLineRepository.findByInvoiceId(invoiceId);

        List<DiscountLineResponse> result = new ArrayList<>();
        for (ChargeLine cl : chargeLines) {
            BigDecimal discount = Optional.ofNullable(cl.getDiscountAmount()).orElse(BigDecimal.ZERO);
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                DiscountLineResponse d = new DiscountLineResponse();
                String description = Optional.ofNullable(cl.getItemDescription()).filter(s -> !s.isBlank()).orElse(cl.getItemCode());
                d.setDescription(description);
                d.setSource(Optional.ofNullable(cl.getDiscountSource()).orElse("LINE"));
                d.setAmount(discount);
                result.add(d);
            }
        }

        // Include invoice-level discount lines
        List<InvoiceDiscountLine> invoiceDiscountLines = invoiceDiscountLineRepository.findByInvoiceId(invoiceId);
        for (InvoiceDiscountLine idl : invoiceDiscountLines) {
            DiscountLineResponse d = new DiscountLineResponse();
            d.setDescription(Optional.ofNullable(idl.getDescription()).orElse("Invoice discount"));
            d.setSource(idl.getSource());
            d.setAmount(idl.getAmount());
            result.add(d);
        }

        return result;
    }

    /**
     * Adds an invoice-level discount line (e.g. from corporate service). Also records a discount audit log entry.
     */
    @Transactional
    public DiscountLineResponse addInvoiceDiscountLine(UUID invoiceId, String description, String source, BigDecimal amount, UUID createdBy) {
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount amount must be positive");
        }

        InvoiceDiscountLine line = new InvoiceDiscountLine();
        line.setId(UUID.randomUUID());
        line.setInvoiceId(invoiceId);
        line.setDescription(description);
        line.setSource(source != null ? source : "INVOICE");
        line.setAmount(amount);
        line.setCreatedAt(OffsetDateTime.now());
        line.setCreatedBy(createdBy);
        invoiceDiscountLineRepository.save(line);

        DiscountAuditLog logEntry = new DiscountAuditLog();
        logEntry.setId(UUID.randomUUID());
        logEntry.setInvoiceId(invoiceId);
        logEntry.setChargeLineId(null);
        logEntry.setDiscountAmount(amount);
        logEntry.setSource(line.getSource());
        logEntry.setAppliedByUserId(createdBy);
        logEntry.setAppliedAt(OffsetDateTime.now());
        discountAuditLogRepository.save(logEntry);

        DiscountLineResponse response = new DiscountLineResponse();
        response.setDescription(Optional.ofNullable(description).orElse("Invoice discount"));
        response.setSource(line.getSource());
        response.setAmount(amount);
        return response;
    }

    private PaymentsSummary buildPaymentsSummary(UUID invoiceId) {
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        if (payments.isEmpty()) {
            PaymentsSummary empty = new PaymentsSummary();
            empty.setTotalPaid(BigDecimal.ZERO);
            empty.setLastPaymentAt(null);
            return empty;
        }

        Map<UUID, BigDecimal> refundedByPayment = refundRepository.findAll().stream()
                .filter(refund -> invoiceId.equals(refund.getInvoiceId()))
                .collect(Collectors.groupingBy(
                        Refund::getOriginalPaymentId,
                        Collectors.mapping(Refund::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        BigDecimal totalPaid = payments.stream()
                .map(payment -> {
                    BigDecimal refunded = refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO);
                    return Optional.ofNullable(payment.getAmount()).orElse(BigDecimal.ZERO).subtract(refunded);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OffsetDateTime lastPaymentAt = payments.stream()
                .map(Payment::getPaymentDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        PaymentsSummary summary = new PaymentsSummary();
        summary.setTotalPaid(totalPaid);
        summary.setLastPaymentAt(lastPaymentAt);
        return summary;
    }
}

