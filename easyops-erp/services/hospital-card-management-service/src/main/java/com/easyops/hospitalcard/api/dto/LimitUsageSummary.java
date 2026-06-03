package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Limit usage for current period (daily or monthly). Populated in GET /cards/{id}/balance when card has a limit profile.
 */
public class LimitUsageSummary {

    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private String resetPolicy;
    private BigDecimal amountConsumed;
    private Integer mealCountConsumed;
    private Integer visitCountConsumed;
    private BigDecimal dailyAmountLimit;
    private BigDecimal monthlyAmountLimit;
    private Integer dailyMealLimit;
    private Integer dailyVisitLimit;

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

    public String getResetPolicy() {
        return resetPolicy;
    }

    public void setResetPolicy(String resetPolicy) {
        this.resetPolicy = resetPolicy;
    }

    public BigDecimal getAmountConsumed() {
        return amountConsumed;
    }

    public void setAmountConsumed(BigDecimal amountConsumed) {
        this.amountConsumed = amountConsumed;
    }

    public Integer getMealCountConsumed() {
        return mealCountConsumed;
    }

    public void setMealCountConsumed(Integer mealCountConsumed) {
        this.mealCountConsumed = mealCountConsumed;
    }

    public Integer getVisitCountConsumed() {
        return visitCountConsumed;
    }

    public void setVisitCountConsumed(Integer visitCountConsumed) {
        this.visitCountConsumed = visitCountConsumed;
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
}
