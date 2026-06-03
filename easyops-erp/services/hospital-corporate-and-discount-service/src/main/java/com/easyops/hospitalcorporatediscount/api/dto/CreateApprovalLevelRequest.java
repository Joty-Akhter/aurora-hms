package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateApprovalLevelRequest {

    @NotBlank
    @Size(max = 100)
    private String roleOrGroupId;

    private BigDecimal maxDiscountPercent;

    private BigDecimal maxDiscountAmount;

    private Integer sortOrder;

    public String getRoleOrGroupId() { return roleOrGroupId; }
    public void setRoleOrGroupId(String roleOrGroupId) { this.roleOrGroupId = roleOrGroupId; }
    public BigDecimal getMaxDiscountPercent() { return maxDiscountPercent; }
    public void setMaxDiscountPercent(BigDecimal maxDiscountPercent) { this.maxDiscountPercent = maxDiscountPercent; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
