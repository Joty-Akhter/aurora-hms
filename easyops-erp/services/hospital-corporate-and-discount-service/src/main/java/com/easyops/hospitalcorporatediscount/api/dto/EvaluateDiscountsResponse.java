package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class EvaluateDiscountsResponse {

    private List<ApplicableSchemeDto> applicableSchemes;
    private BigDecimal recommendedTotalDiscount;
    private Boolean requiresApproval;
    private String message;

    public List<ApplicableSchemeDto> getApplicableSchemes() { return applicableSchemes; }
    public void setApplicableSchemes(List<ApplicableSchemeDto> applicableSchemes) { this.applicableSchemes = applicableSchemes; }
    public BigDecimal getRecommendedTotalDiscount() { return recommendedTotalDiscount; }
    public void setRecommendedTotalDiscount(BigDecimal recommendedTotalDiscount) { this.recommendedTotalDiscount = recommendedTotalDiscount; }
    public Boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(Boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
