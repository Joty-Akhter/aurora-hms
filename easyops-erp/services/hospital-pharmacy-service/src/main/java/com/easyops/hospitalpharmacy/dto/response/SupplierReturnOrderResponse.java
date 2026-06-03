package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SupplierReturnOrderResponse {

    private UUID id;
    private UUID manufacturerId;
    private String manufacturerName;
    private UUID fromLocationId;
    private String fromLocationName;
    private String status;
    private String returnReference;
    private UUID requestedBy;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime dispatchedAt;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<SupplierReturnOrderLineResponse> lines;
}
