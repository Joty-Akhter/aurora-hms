package com.easyops.hospitalscheduling.api.dto;

import jakarta.validation.constraints.Size;

public class CancelAppointmentRequest {

    @Size(max = 500)
    private String reason;

    @Size(max = 255)
    private String idempotencyKey;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
