package com.easyops.hospitalbilling.domain.invoice;

import com.easyops.hospitalbilling.api.dto.EstimateLineItemRequest;
import com.easyops.hospitalbilling.api.dto.EstimateRequest;
import com.easyops.hospitalbilling.integration.CorporateCardValidationClient;
import com.easyops.hospitalbilling.integration.DiscountRulesClient;
import com.easyops.hospitalbilling.integration.dto.CorporateCardValidationResponse;
import com.easyops.hospitalbilling.integration.dto.EvaluateDiscountsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceEstimateTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private com.easyops.hospitalbilling.domain.charge.ChargeLineRepository chargeLineRepository;
    @Mock
    private com.easyops.hospitalbilling.domain.payment.PaymentRepository paymentRepository;
    @Mock
    private com.easyops.hospitalbilling.domain.payment.RefundRepository refundRepository;
    @Mock
    private com.easyops.hospitalbilling.domain.adjustment.AdjustmentRepository adjustmentRepository;
    @Mock
    private InvoiceDiscountLineRepository invoiceDiscountLineRepository;
    @Mock
    private DiscountAuditLogRepository discountAuditLogRepository;
    @Mock
    private DiscountRulesClient discountRulesClient;
    @Mock
    private CorporateCardValidationClient corporateCardValidationClient;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        CommunicationInvoiceEventPublisher communicationInvoiceEventPublisher = new CommunicationInvoiceEventPublisher(
                null,
                new ObjectMapper(),
                new SimpleMeterRegistry(),
                false,
                "invoice-lifecycle-events",
                "00000000-0000-0000-0000-000000000000"
        );
        LegacyInvoiceNotificationFallbackService legacyInvoiceNotificationFallbackService = new LegacyInvoiceNotificationFallbackService();

        invoiceService = new InvoiceService(
                invoiceRepository,
                chargeLineRepository,
                paymentRepository,
                refundRepository,
                adjustmentRepository,
                invoiceDiscountLineRepository,
                discountAuditLogRepository,
                discountRulesClient,
                corporateCardValidationClient,
                new SimpleMeterRegistry(),
                communicationInvoiceEventPublisher,
                legacyInvoiceNotificationFallbackService
        );
    }

    @Test
    void computeEstimate_rejectsInvalidCorporateCard() {
        EstimateRequest request = new EstimateRequest();
        request.setCardNumber("BAD-CARD");
        request.setLineItems(List.of(line("CONSULT", "Consultation", "1", "100")));

        CorporateCardValidationResponse invalid = new CorporateCardValidationResponse();
        invalid.setValid(false);
        invalid.setMessage("Card is blocked");
        when(corporateCardValidationClient.validateCard("BAD-CARD")).thenReturn(invalid);

        assertThatThrownBy(() -> invoiceService.computeEstimate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid corporate card")
                .hasMessageContaining("Card is blocked");
    }

    @Test
    void computeEstimate_distributesRecommendedTotalWhenNoPerLineDiscounts() {
        EstimateRequest request = new EstimateRequest();
        request.setCorporateClientId(UUID.randomUUID());
        request.setLineItems(List.of(
                line("CONSULT", "Consultation", "1", "100"),
                line("LAB", "Lab", "1", "300")
        ));

        EvaluateDiscountsResponse evaluateResponse = new EvaluateDiscountsResponse();
        evaluateResponse.setRecommendedTotalDiscount(new BigDecimal("40.00"));
        evaluateResponse.setLineDiscounts(List.of()); // no per-line discounts returned
        when(discountRulesClient.evaluateDiscounts(any())).thenReturn(evaluateResponse);

        var response = invoiceService.computeEstimate(request);

        assertThat(response.getTotalDiscount()).isEqualByComparingTo("40.0000");
        assertThat(response.getLines()).hasSize(2);
        assertThat(response.getLines().get(0).getDiscountAmount()).isEqualByComparingTo("10.0000");
        assertThat(response.getLines().get(1).getDiscountAmount()).isEqualByComparingTo("30.0000");
        assertThat(response.getNetPayable()).isEqualByComparingTo("360.0000");
    }

    private static EstimateLineItemRequest line(String code, String description, String qty, String unitPrice) {
        EstimateLineItemRequest item = new EstimateLineItemRequest();
        item.setItemCode(code);
        item.setItemDescription(description);
        item.setQuantity(new BigDecimal(qty));
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
