package com.easyops.hospitalcard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateLimitProfileRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private BigDecimal dailyAmountLimit;

    private BigDecimal monthlyAmountLimit;

    private Integer dailyMealLimit;

    private Integer dailyVisitLimit;

    @NotNull(message = "resetPolicy is required")
    private String resetPolicy;

    private String currency;

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
}
