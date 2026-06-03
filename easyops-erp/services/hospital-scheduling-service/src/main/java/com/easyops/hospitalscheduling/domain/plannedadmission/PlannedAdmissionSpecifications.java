package com.easyops.hospitalscheduling.domain.plannedadmission;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PlannedAdmissionSpecifications {

    private PlannedAdmissionSpecifications() {}

    public static Specification<PlannedAdmission> hasPatientId(UUID patientId) {
        if (patientId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<PlannedAdmission> preferredDateFrom(LocalDate from) {
        if (from == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("preferredDate"), from);
    }

    public static Specification<PlannedAdmission> preferredDateTo(LocalDate to) {
        if (to == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("preferredDate"), to);
    }

    public static Specification<PlannedAdmission> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }
}
