package com.easyops.hospitalclinicalorders.domain.order;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClinicalOrderSpecifications {

    private ClinicalOrderSpecifications() {}

    public static Specification<ClinicalOrder> hasOrderSetId(UUID orderSetId) {
        if (orderSetId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("orderSetId"), orderSetId);
    }

    public static Specification<ClinicalOrder> hasFacilityId(UUID facilityId) {
        if (facilityId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("facilityId"), facilityId);
    }

    public static Specification<ClinicalOrder> hasPatientIdViaOrderSet(UUID patientId) {
        if (patientId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> {
            var orderSet = root.join("orderSet");
            return cb.equal(orderSet.get("patientId"), patientId);
        };
    }

    public static Specification<ClinicalOrder> hasVisitIdViaOrderSet(UUID visitId) {
        if (visitId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> {
            var orderSet = root.join("orderSet");
            return cb.equal(orderSet.get("visitId"), visitId);
        };
    }

    public static Specification<ClinicalOrder> hasOrderType(String type) {
        if (type == null || type.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("orderType"), type);
    }

    public static Specification<ClinicalOrder> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ClinicalOrder> createdAtBetween(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}
