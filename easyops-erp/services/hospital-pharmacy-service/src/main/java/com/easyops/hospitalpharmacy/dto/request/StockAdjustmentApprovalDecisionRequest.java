package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StockAdjustmentApprovalDecisionRequest {

    /** Expected values: "APPROVED" or "REJECTED". */
    @NotBlank
    private String decision;

    private String rejectionReason;
}
