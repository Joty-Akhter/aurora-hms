package com.easyops.hospitalclinicalorders.domain.orderset;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class OrderSetSpecifications {

    private OrderSetSpecifications() {}

    public static Specification<OrderSet> hasFacilityId(UUID facilityId) {
        if (facilityId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("facilityId"), facilityId);
    }

    public static Specification<OrderSet> hasPatientId(UUID patientId) {
        if (patientId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<OrderSet> hasVisitId(UUID visitId) {
        if (visitId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("visitId"), visitId);
    }

    public static Specification<OrderSet> createdAtBetween(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}
