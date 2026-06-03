package com.easyops.hospitalbilling.api.dto;

import java.util.List;

public class PaymentDetailResponse {

    private PaymentResponse payment;
    private List<RefundResponse> refunds;

    public PaymentResponse getPayment() {
        return payment;
    }

    public void setPayment(PaymentResponse payment) {
        this.payment = payment;
    }

    public List<RefundResponse> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<RefundResponse> refunds) {
        this.refunds = refunds;
    }
}

