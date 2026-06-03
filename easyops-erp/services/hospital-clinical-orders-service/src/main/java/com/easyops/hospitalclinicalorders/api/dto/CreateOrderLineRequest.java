package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateOrderLineRequest {
    @NotNull
    private String orderType; // LAB, RADIOLOGY, PROCEDURE
    @NotBlank
    private String itemCode;
    private String orderingNotes;
    private String priority;

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getOrderingNotes() { return orderingNotes; }
    public void setOrderingNotes(String orderingNotes) { this.orderingNotes = orderingNotes; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
