package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscountApprovalLevelResponse {

    private UUID id;
    private UUID discountSchemeId;
    private String roleOrGroupId;
    private BigDecimal maxDiscountPercent;
    private BigDecimal maxDiscountAmount;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDiscountSchemeId() { return discountSchemeId; }
    public void setDiscountSchemeId(UUID discountSchemeId) { this.discountSchemeId = discountSchemeId; }
    public String getRoleOrGroupId() { return roleOrGroupId; }
    public void setRoleOrGroupId(String roleOrGroupId) { this.roleOrGroupId = roleOrGroupId; }
    public BigDecimal getMaxDiscountPercent() { return maxDiscountPercent; }
    public void setMaxDiscountPercent(BigDecimal maxDiscountPercent) { this.maxDiscountPercent = maxDiscountPercent; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
