package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PharmacyPaymentResponse {

    private UUID id;
    private UUID creditAccountId;
    private UUID dispenseOrderId;
    private BigDecimal amount;
    private String paymentMode;
    private String referenceNo;
    private UUID receivedBy;
    private OffsetDateTime paymentDate;
    private String notes;
    private OffsetDateTime createdAt;
}
