package com.easyops.hospitalcorporatediscount.domain.discount;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "discount_approval_levels", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class DiscountApprovalLevel {

    @Id
    private UUID id;

    @Column(name = "discount_scheme_id", nullable = false)
    private UUID discountSchemeId;

    @Column(name = "role_or_group_id", nullable = false, length = 100)
    private String roleOrGroupId;

    @Column(name = "max_discount_percent", precision = 5, scale = 2)
    private BigDecimal maxDiscountPercent;

    @Column(name = "max_discount_amount", precision = 19, scale = 4)
    private BigDecimal maxDiscountAmount;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    @CreatedDate
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

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
