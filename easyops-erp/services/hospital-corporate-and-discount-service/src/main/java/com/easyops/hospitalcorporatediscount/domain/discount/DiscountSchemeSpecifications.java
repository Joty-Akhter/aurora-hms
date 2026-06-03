package com.easyops.hospitalcorporatediscount.domain.discount;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class DiscountSchemeSpecifications {

    private DiscountSchemeSpecifications() {}

    public static Specification<DiscountScheme> activeAndValidNow() {
        LocalDate today = LocalDate.now();
        return (root, query, cb) -> cb.and(
                cb.equal(cb.upper(root.get("status")), "ACTIVE"),
                cb.lessThanOrEqualTo(root.get("validFrom"), today),
                cb.or(
                        cb.isNull(root.get("validTo")),
                        cb.greaterThanOrEqualTo(root.get("validTo"), today)
                )
        );
    }

    public static Specification<DiscountScheme> corporateOrGeneral(UUID corporateClientId) {
        if (corporateClientId == null) {
            return (root, query, cb) -> cb.isNull(root.get("corporateClientId"));
        }
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("corporateClientId")),
                cb.equal(root.get("corporateClientId"), corporateClientId)
        );
    }

    public static Specification<DiscountScheme> visitTypeMatches(String visitType) {
        if (visitType == null || visitType.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("visitType")),
                cb.equal(cb.upper(root.get("visitType")), visitType.trim().toUpperCase())
        );
    }

    public static Specification<DiscountScheme> departmentMatches(UUID departmentId) {
        if (departmentId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("departmentId")),
                cb.equal(root.get("departmentId"), departmentId)
        );
    }

    public static Specification<DiscountScheme> hasCode(String code) {
        if (code == null || code.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String trimmed = code.trim();
        return (root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + trimmed.toLowerCase() + "%");
    }

    public static Specification<DiscountScheme> hasCorporateClientId(UUID corporateClientId) {
        if (corporateClientId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("corporateClientId"), corporateClientId);
    }

    public static Specification<DiscountScheme> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }
}
