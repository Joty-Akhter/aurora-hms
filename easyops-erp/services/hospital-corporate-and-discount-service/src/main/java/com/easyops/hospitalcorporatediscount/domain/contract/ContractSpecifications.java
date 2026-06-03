package com.easyops.hospitalcorporatediscount.domain.contract;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Status filter is derived from validFrom/validTo when no explicit status column exists:
 * ACTIVE = validFrom <= today AND (validTo is null OR validTo >= today)
 * EXPIRED = validTo is not null AND validTo < today
 * FUTURE = validFrom > today
 */
public final class ContractSpecifications {

    private static final LocalDate TODAY = LocalDate.now();

    private ContractSpecifications() {}

    public static Specification<CorporateContract> hasCorporateClientId(java.util.UUID corporateClientId) {
        if (corporateClientId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("corporateClientId"), corporateClientId);
    }

    public static Specification<CorporateContract> hasEffectiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> (root, query, cb) -> cb.and(
                    cb.lessThanOrEqualTo(root.get("validFrom"), TODAY),
                    cb.or(
                            cb.isNull(root.get("validTo")),
                            cb.greaterThanOrEqualTo(root.get("validTo"), TODAY)
                    )
            );
            case "EXPIRED" -> (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get("validTo")),
                    cb.lessThan(root.get("validTo"), TODAY)
            );
            case "FUTURE" -> (root, query, cb) -> cb.greaterThan(root.get("validFrom"), TODAY);
            default -> (root, query, cb) -> cb.conjunction();
        };
    }
}
