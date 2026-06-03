package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PharmacyPaymentRequest {

    @NotNull
    private UUID creditAccountId;

    private UUID dispenseOrderId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 50)
    private String paymentMode;

    @Size(max = 100)
    private String referenceNo;

    private String notes;
}
