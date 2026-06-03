package com.easyops.hospitalcard.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a card is closed. Event name: {@code card.closed}
 */
public class CardClosedEvent {

    private final UUID cardId;
    private final BigDecimal finalBalance;
    private final String currency;

    public CardClosedEvent(UUID cardId, BigDecimal finalBalance, String currency) {
        this.cardId = cardId;
        this.finalBalance = finalBalance != null ? finalBalance : BigDecimal.ZERO;
        this.currency = currency;
    }

    public UUID getCardId() {
        return cardId;
    }

    public BigDecimal getFinalBalance() {
        return finalBalance;
    }

    public String getCurrency() {
        return currency;
    }
}
