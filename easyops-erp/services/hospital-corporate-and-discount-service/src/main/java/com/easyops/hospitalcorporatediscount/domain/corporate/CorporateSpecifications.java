package com.easyops.hospitalcorporatediscount.domain.corporate;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CorporateSpecifications {

    private CorporateSpecifications() {}

    public static Specification<CorporateClient> hasCode(String code) {
        if (code == null || code.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(cb.lower(root.get("code")), code.toLowerCase().trim());
    }

    public static Specification<CorporateClient> hasType(String type) {
        if (type == null || type.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<CorporateClient> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }
}
