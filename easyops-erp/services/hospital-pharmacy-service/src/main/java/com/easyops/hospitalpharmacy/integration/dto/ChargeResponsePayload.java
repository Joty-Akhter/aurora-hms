package com.easyops.hospitalpharmacy.integration.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ChargeResponsePayload {
    private UUID id;
    private String sourceService;
    private String sourceReferenceId;
    private UUID patientId;
    private BigDecimal netAmount;
    private String status;
}
