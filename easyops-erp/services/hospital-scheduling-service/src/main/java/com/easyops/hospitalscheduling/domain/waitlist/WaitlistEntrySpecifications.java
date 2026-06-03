package com.easyops.hospitalscheduling.domain.waitlist;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class WaitlistEntrySpecifications {

    private WaitlistEntrySpecifications() {}

    public static Specification<WaitlistEntry> hasResourceId(UUID resourceId) {
        return (root, query, cb) -> resourceId == null ? cb.conjunction() : cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<WaitlistEntry> hasPatientId(UUID patientId) {
        return (root, query, cb) -> patientId == null ? cb.conjunction() : cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<WaitlistEntry> hasStatus(String status) {
        return (root, query, cb) -> status == null || status.isBlank() ? cb.conjunction() : cb.equal(root.get("status"), status.trim());
    }
}
