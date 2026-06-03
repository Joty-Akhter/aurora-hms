package com.easyops.hospitalcorporatediscount.api.dto;

import java.util.List;

public class DiscountSchemeDetailResponse extends DiscountSchemeResponse {

    private List<DiscountApprovalLevelResponse> approvalLevels;

    public List<DiscountApprovalLevelResponse> getApprovalLevels() { return approvalLevels; }
    public void setApprovalLevels(List<DiscountApprovalLevelResponse> approvalLevels) { this.approvalLevels = approvalLevels; }
}
