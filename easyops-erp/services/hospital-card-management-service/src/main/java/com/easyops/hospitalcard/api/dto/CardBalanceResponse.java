package com.easyops.hospitalcard.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CardBalanceResponse {

    private UUID cardId;
    private UUID accountId;
    private String accountType;
    private BigDecimal currentBalance;
    private String currency;
    private BigDecimal creditLimit;
    private UUID limitProfileId; // optional, from card
    private LimitUsageSummary limitUsage; // Phase 3: current period consumed vs limits

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public UUID getLimitProfileId() {
        return limitProfileId;
    }

    public void setLimitProfileId(UUID limitProfileId) {
        this.limitProfileId = limitProfileId;
    }

    public LimitUsageSummary getLimitUsage() {
        return limitUsage;
    }

    public void setLimitUsage(LimitUsageSummary limitUsage) {
        this.limitUsage = limitUsage;
    }
}
