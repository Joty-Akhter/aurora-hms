package com.easyops.hospitalcorporatediscount.domain.corporatecard;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CorporateCardSpecifications {
    private CorporateCardSpecifications() {}

    public static Specification<CorporateCard> hasCorporateClientId(UUID corporateClientId) {
        return (root, query, cb) -> corporateClientId == null ? null : cb.equal(root.get("corporateClientId"), corporateClientId);
    }

    public static Specification<CorporateCard> hasHolderIdentifier(String holderIdentifier) {
        return (root, query, cb) -> (holderIdentifier == null || holderIdentifier.isBlank())
                ? null
                : cb.equal(cb.lower(root.get("holderIdentifier")), holderIdentifier.trim().toLowerCase());
    }

    public static Specification<CorporateCard> hasStatus(String status) {
        return (root, query, cb) -> (status == null || status.isBlank())
                ? null
                : cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }
}
