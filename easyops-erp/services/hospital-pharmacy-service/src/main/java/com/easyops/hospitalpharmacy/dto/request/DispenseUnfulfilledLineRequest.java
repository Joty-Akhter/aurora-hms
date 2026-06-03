package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DispenseUnfulfilledLineRequest {

    @NotNull
    private UUID drugId;

    private UUID prescriptionLineId;

    private BigDecimal quantityPrescribed;

    /** Must be OUT_OF_STOCK or REFUSED */
    @NotBlank
    private String lineStatus;

    @NotBlank
    private String reasonCode;

    private UUID documentingUserId;
}
