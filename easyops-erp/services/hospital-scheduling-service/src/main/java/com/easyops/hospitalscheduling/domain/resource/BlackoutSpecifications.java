package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class BlackoutSpecifications {

    private BlackoutSpecifications() {}

    public static Specification<Blackout> hasResourceId(UUID resourceId) {
        if (resourceId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<Blackout> hasBranchId(UUID branchId) {
        if (branchId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<Blackout> blackoutDateFrom(LocalDate fromDate) {
        if (fromDate == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("blackoutDate"), fromDate);
    }

    public static Specification<Blackout> blackoutDateTo(LocalDate toDate) {
        if (toDate == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("blackoutDate"), toDate);
    }
}
