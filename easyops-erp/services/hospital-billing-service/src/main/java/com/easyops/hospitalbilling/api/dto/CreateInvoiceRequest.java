package com.easyops.hospitalbilling.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreateInvoiceRequest {
    private UUID patientId;
    private UUID visitId;
    private String payerType;
    private UUID payerId;
    private List<UUID> chargeLineIds;
    private String groupBy;
    private LocalDate dueDate;

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public String getPayerType() {
        return payerType;
    }

    public void setPayerType(String payerType) {
        this.payerType = payerType;
    }

    public UUID getPayerId() {
        return payerId;
    }

    public void setPayerId(UUID payerId) {
        this.payerId = payerId;
    }

    public List<UUID> getChargeLineIds() {
        return chargeLineIds;
    }

    public void setChargeLineIds(List<UUID> chargeLineIds) {
        this.chargeLineIds = chargeLineIds;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}

