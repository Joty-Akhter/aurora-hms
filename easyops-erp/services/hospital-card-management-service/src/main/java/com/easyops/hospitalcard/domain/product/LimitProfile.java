package com.easyops.hospitalcard.domain.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "limit_profiles", schema = "hospital_card")
public class LimitProfile {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "daily_amount_limit", precision = 19, scale = 4)
    private BigDecimal dailyAmountLimit;

    @Column(name = "monthly_amount_limit", precision = 19, scale = 4)
    private BigDecimal monthlyAmountLimit;

    @Column(name = "daily_meal_limit")
    private Integer dailyMealLimit;

    @Column(name = "daily_visit_limit")
    private Integer dailyVisitLimit;

    @Column(name = "reset_policy", nullable = false, length = 30)
    private String resetPolicy;

    @Column(name = "currency", length = 3)
    private String currency;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDailyAmountLimit() {
        return dailyAmountLimit;
    }

    public void setDailyAmountLimit(BigDecimal dailyAmountLimit) {
        this.dailyAmountLimit = dailyAmountLimit;
    }

    public BigDecimal getMonthlyAmountLimit() {
        return monthlyAmountLimit;
    }

    public void setMonthlyAmountLimit(BigDecimal monthlyAmountLimit) {
        this.monthlyAmountLimit = monthlyAmountLimit;
    }

    public Integer getDailyMealLimit() {
        return dailyMealLimit;
    }

    public void setDailyMealLimit(Integer dailyMealLimit) {
        this.dailyMealLimit = dailyMealLimit;
    }

    public Integer getDailyVisitLimit() {
        return dailyVisitLimit;
    }

    public void setDailyVisitLimit(Integer dailyVisitLimit) {
        this.dailyVisitLimit = dailyVisitLimit;
    }

    public String getResetPolicy() {
        return resetPolicy;
    }

    public void setResetPolicy(String resetPolicy) {
        this.resetPolicy = resetPolicy;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
