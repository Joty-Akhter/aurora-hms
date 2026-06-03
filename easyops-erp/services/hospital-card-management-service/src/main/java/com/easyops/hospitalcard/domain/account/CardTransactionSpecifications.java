package com.easyops.hospitalcard.domain.account;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class CardTransactionSpecifications {

    private CardTransactionSpecifications() {
    }

    public static Specification<CardTransaction> hasCardAccountId(UUID cardAccountId) {
        if (cardAccountId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("cardAccountId"), cardAccountId);
    }

    public static Specification<CardTransaction> createdFrom(OffsetDateTime from) {
        if (from == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<CardTransaction> createdTo(OffsetDateTime to) {
        if (to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<CardTransaction> hasTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("transactionType"), type);
    }

    public static Specification<CardTransaction> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CardTransaction> hasSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("sourceSystem"), sourceSystem);
    }

    public static Specification<CardTransaction> postedFrom(OffsetDateTime from) {
        if (from == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("postedAt"), from);
    }

    public static Specification<CardTransaction> postedTo(OffsetDateTime to) {
        if (to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("postedAt"), to);
    }
}
