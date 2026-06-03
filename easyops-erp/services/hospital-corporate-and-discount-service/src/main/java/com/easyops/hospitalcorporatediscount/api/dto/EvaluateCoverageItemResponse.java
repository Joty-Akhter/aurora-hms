package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class EvaluateCoverageItemResponse {

    private int lineIndex;
    private String serviceCode;
    private BigDecimal coveredPercent;
    private BigDecimal coveredAmount;
    private BigDecimal patientShare;
    private BigDecimal corporateShare;
    private BigDecimal maxApplicable;
    private UUID ruleId;

    public int getLineIndex() { return lineIndex; }
    public void setLineIndex(int lineIndex) { this.lineIndex = lineIndex; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public BigDecimal getCoveredPercent() { return coveredPercent; }
    public void setCoveredPercent(BigDecimal coveredPercent) { this.coveredPercent = coveredPercent; }
    public BigDecimal getCoveredAmount() { return coveredAmount; }
    public void setCoveredAmount(BigDecimal coveredAmount) { this.coveredAmount = coveredAmount; }
    public BigDecimal getPatientShare() { return patientShare; }
    public void setPatientShare(BigDecimal patientShare) { this.patientShare = patientShare; }
    public BigDecimal getCorporateShare() { return corporateShare; }
    public void setCorporateShare(BigDecimal corporateShare) { this.corporateShare = corporateShare; }
    public BigDecimal getMaxApplicable() { return maxApplicable; }
    public void setMaxApplicable(BigDecimal maxApplicable) { this.maxApplicable = maxApplicable; }
    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }
}
