package com.easyops.hospitalcard.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a card account balance changes (e.g. after capture, top-up, refund).
 * Event name: {@code card.balance.changed}
 */
public class CardBalanceChangedEvent {

    private final UUID cardId;
    private final UUID accountId;
    private final BigDecimal currentBalance;
    private final String currency;

    public CardBalanceChangedEvent(UUID cardId, UUID accountId, BigDecimal currentBalance, String currency) {
        this.cardId = cardId;
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.currency = currency;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public String getCurrency() {
        return currency;
    }
}
