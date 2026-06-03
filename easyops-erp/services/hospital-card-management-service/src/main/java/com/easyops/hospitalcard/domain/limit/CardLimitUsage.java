package com.easyops.hospitalcard.domain.limit;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_limit_usage", schema = "hospital_card")
public class CardLimitUsage {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "limit_profile_id", nullable = false)
    private UUID limitProfileId;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "amount_consumed", precision = 19, scale = 4)
    private BigDecimal amountConsumed;

    @Column(name = "meal_count_consumed")
    private Integer mealCountConsumed;

    @Column(name = "visit_count_consumed")
    private Integer visitCountConsumed;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public UUID getLimitProfileId() {
        return limitProfileId;
    }

    public void setLimitProfileId(UUID limitProfileId) {
        this.limitProfileId = limitProfileId;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(OffsetDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(OffsetDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getAmountConsumed() {
        return amountConsumed != null ? amountConsumed : BigDecimal.ZERO;
    }

    public void setAmountConsumed(BigDecimal amountConsumed) {
        this.amountConsumed = amountConsumed;
    }

    public Integer getMealCountConsumed() {
        return mealCountConsumed != null ? mealCountConsumed : 0;
    }

    public void setMealCountConsumed(Integer mealCountConsumed) {
        this.mealCountConsumed = mealCountConsumed;
    }

    public Integer getVisitCountConsumed() {
        return visitCountConsumed != null ? visitCountConsumed : 0;
    }

    public void setVisitCountConsumed(Integer visitCountConsumed) {
        this.visitCountConsumed = visitCountConsumed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
