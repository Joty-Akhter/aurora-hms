package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StockAdjustmentApprovalResponse {

    private UUID id;
    private UUID pharmacyLocationId;
    private UUID requestedBy;
    private String status;
    private String requestPayload;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;
    private String rejectionReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
