package com.easyops.hospitalbilling.api.dto;

import java.util.List;

public class InvoiceDetailResponse {

    private InvoiceResponse invoice;
    private List<ChargeResponse> chargeLines;
    private PaymentsSummary paymentsSummary;

    public InvoiceResponse getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceResponse invoice) {
        this.invoice = invoice;
    }

    public List<ChargeResponse> getChargeLines() {
        return chargeLines;
    }

    public void setChargeLines(List<ChargeResponse> chargeLines) {
        this.chargeLines = chargeLines;
    }

    public PaymentsSummary getPaymentsSummary() {
        return paymentsSummary;
    }

    public void setPaymentsSummary(PaymentsSummary paymentsSummary) {
        this.paymentsSummary = paymentsSummary;
    }
}

