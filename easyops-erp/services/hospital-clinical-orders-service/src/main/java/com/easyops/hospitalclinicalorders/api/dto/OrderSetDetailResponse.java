package com.easyops.hospitalclinicalorders.api.dto;

import java.util.ArrayList;
import java.util.List;

public class OrderSetDetailResponse extends OrderSetResponse {
    private List<ClinicalOrderResponse> orders = new ArrayList<>();

    public List<ClinicalOrderResponse> getOrders() { return orders; }
    public void setOrders(List<ClinicalOrderResponse> orders) { this.orders = orders != null ? orders : new ArrayList<>(); }
}
