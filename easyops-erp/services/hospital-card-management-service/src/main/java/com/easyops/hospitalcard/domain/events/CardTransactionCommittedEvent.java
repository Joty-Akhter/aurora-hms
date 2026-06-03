package com.easyops.hospitalcard.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after a card transaction is committed (e.g. capture).
 * Event name: {@code card.transaction.committed}
 */
public class CardTransactionCommittedEvent {

    private final UUID cardId;
    private final UUID transactionId;
    private final BigDecimal amount;
    private final String sourceSystem;
    private final String externalReferenceId;

    public CardTransactionCommittedEvent(UUID cardId, UUID transactionId, BigDecimal amount,
                                         String sourceSystem, String externalReferenceId) {
        this.cardId = cardId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.sourceSystem = sourceSystem;
        this.externalReferenceId = externalReferenceId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getExternalReferenceId() {
        return externalReferenceId;
    }
}
