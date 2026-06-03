package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StockRequisitionResponse {

    private UUID id;
    private UUID fromLocationId;
    private String fromLocationName;
    private UUID toLocationId;
    private String toLocationName;
    private String status;
    private UUID requestedBy;
    private OffsetDateTime submittedAt;
    private UUID approvedBy;
    private String approvalNotes;
    private OffsetDateTime approvedAt;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<StockRequisitionLineResponse> lines;
}
