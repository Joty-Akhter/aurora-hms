package com.easyops.hospitalcard.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a card is replaced (e.g. lost/damaged). Event name: {@code card.replaced}
 */
public class CardReplacedEvent {

    private final UUID oldCardId;
    private final UUID newCardId;
    private final BigDecimal transferredBalance;

    public CardReplacedEvent(UUID oldCardId, UUID newCardId, BigDecimal transferredBalance) {
        this.oldCardId = oldCardId;
        this.newCardId = newCardId;
        this.transferredBalance = transferredBalance != null ? transferredBalance : BigDecimal.ZERO;
    }

    public UUID getOldCardId() {
        return oldCardId;
    }

    public UUID getNewCardId() {
        return newCardId;
    }

    public BigDecimal getTransferredBalance() {
        return transferredBalance;
    }
}
