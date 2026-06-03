package com.easyops.hospitalscheduling.domain.reservation;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ReservationSpecifications {

    private ReservationSpecifications() {}

    public static Specification<Reservation> hasResourceId(UUID resourceId) {
        if (resourceId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<Reservation> hasPatientId(UUID patientId) {
        if (patientId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<Reservation> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> slotStartFrom(OffsetDateTime from) {
        if (from == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("slotStart"), from);
    }

    public static Specification<Reservation> slotEndTo(OffsetDateTime to) {
        if (to == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("slotEnd"), to);
    }
}
