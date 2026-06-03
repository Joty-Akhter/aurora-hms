package com.easyops.hospitalcard.domain.card;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> hasCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("cardNumber")), cardNumber.toLowerCase());
    }

    public static Specification<Card> hasOwnerReferenceId(String ownerReferenceId) {
        if (ownerReferenceId == null || ownerReferenceId.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("ownerReferenceId")), ownerReferenceId.toLowerCase());
    }

    public static Specification<Card> hasOwnerType(String ownerType) {
        if (ownerType == null || ownerType.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("ownerType"), ownerType);
    }

    public static Specification<Card> hasCorporateId(java.util.UUID corporateId) {
        if (corporateId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("corporateId"), corporateId);
    }

    public static Specification<Card> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Card> hasCardProductId(UUID cardProductId) {
        if (cardProductId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("cardProductId"), cardProductId);
    }

    public static Specification<Card> issuedAtBetween(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("issuedAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("issuedAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("issuedAt"), to);
        };
    }
}
