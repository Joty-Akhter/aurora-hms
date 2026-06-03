package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PharmacyCreditAccountRequest {

    @NotNull
    private UUID patientId;

    private String customerName;

    @NotNull
    @PositiveOrZero
    private BigDecimal creditLimit;

    private String notes;
}
