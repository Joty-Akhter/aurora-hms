package com.easyops.hospitalpharmacy.integration.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateChargePayload {
    private String sourceService;
    private String sourceReferenceId;
    private UUID patientId;
    private UUID visitId;
    private UUID corporateContractId;
    private String itemCode;
    private String itemDescription;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private String discountSource;
    private BigDecimal taxAmount;
    private String idempotencyKey;
}
