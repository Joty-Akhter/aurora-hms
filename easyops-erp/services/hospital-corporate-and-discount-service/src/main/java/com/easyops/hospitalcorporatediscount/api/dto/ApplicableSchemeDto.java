package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ApplicableSchemeDto {

    private UUID schemeId;
    private String schemeCode;
    private BigDecimal recommendedPercent;
    private BigDecimal recommendedAmount;
    private BigDecimal cappedAmount;
    private Boolean requiresApproval;
    private String requiredApprovalLevel;

    public UUID getSchemeId() { return schemeId; }
    public void setSchemeId(UUID schemeId) { this.schemeId = schemeId; }
    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }
    public BigDecimal getRecommendedPercent() { return recommendedPercent; }
    public void setRecommendedPercent(BigDecimal recommendedPercent) { this.recommendedPercent = recommendedPercent; }
    public BigDecimal getRecommendedAmount() { return recommendedAmount; }
    public void setRecommendedAmount(BigDecimal recommendedAmount) { this.recommendedAmount = recommendedAmount; }
    public BigDecimal getCappedAmount() { return cappedAmount; }
    public void setCappedAmount(BigDecimal cappedAmount) { this.cappedAmount = cappedAmount; }
    public Boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(Boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public String getRequiredApprovalLevel() { return requiredApprovalLevel; }
    public void setRequiredApprovalLevel(String requiredApprovalLevel) { this.requiredApprovalLevel = requiredApprovalLevel; }
}
