package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class StockRequisitionApprovalRequest {

    private String approvalNotes;

    private List<LineApproval> lineApprovals;

    @Data
    public static class LineApproval {

        private UUID lineId;

        @PositiveOrZero
        private BigDecimal approvedQuantity;
    }
}
