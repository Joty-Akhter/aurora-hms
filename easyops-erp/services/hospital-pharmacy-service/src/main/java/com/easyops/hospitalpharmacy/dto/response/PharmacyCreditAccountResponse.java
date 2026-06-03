package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PharmacyCreditAccountResponse {

    private UUID id;
    private UUID patientId;
    private String customerName;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private BigDecimal availableCredit;
    private boolean active;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
