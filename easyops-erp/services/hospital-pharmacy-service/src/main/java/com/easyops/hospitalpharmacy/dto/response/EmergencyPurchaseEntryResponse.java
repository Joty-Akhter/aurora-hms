package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EmergencyPurchaseEntryResponse {

    private UUID id;
    private UUID toLocationId;
    private String toLocationName;
    private String status;
    private String reason;
    private String supplierName;
    private String invoiceRef;
    private UUID requestedBy;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime receivedAt;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<EmergencyPurchaseLineResponse> lines;
}
