package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class LimitProfileResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal dailyAmountLimit;
    private BigDecimal monthlyAmountLimit;
    private Integer dailyMealLimit;
    private Integer dailyVisitLimit;
    private String resetPolicy;
    private String currency;
    private OffsetDateTime createdAt;
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
