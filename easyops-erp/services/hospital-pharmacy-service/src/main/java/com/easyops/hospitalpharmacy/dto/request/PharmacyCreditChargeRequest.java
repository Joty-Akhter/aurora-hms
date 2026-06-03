package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PharmacyCreditChargeRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    private UUID dispenseOrderId;
}
