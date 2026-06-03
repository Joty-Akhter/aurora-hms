package com.easyops.hospitalcard.domain.product;

import org.springframework.data.jpa.domain.Specification;

public final class CardProductSpecifications {

    private CardProductSpecifications() {
    }

    public static Specification<CardProduct> hasCode(String code) {
        if (code == null || code.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("code")), code.toLowerCase());
    }

    public static Specification<CardProduct> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
