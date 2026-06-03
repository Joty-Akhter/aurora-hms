package com.easyops.hospitalclinicalorders.api.dto;

public class UpdateOrderRequest {
    private String priority;
    private String orderingNotes;

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getOrderingNotes() { return orderingNotes; }
    public void setOrderingNotes(String orderingNotes) { this.orderingNotes = orderingNotes; }
}
