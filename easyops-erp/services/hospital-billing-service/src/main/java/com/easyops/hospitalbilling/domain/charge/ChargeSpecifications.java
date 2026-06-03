package com.easyops.hospitalbilling.domain.charge;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ChargeSpecifications {

    private ChargeSpecifications() {
    }

    public static Specification<ChargeLine> hasPatientId(UUID patientId) {
        return (root, query, cb) ->
                patientId == null ? null : cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<ChargeLine> hasVisitId(UUID visitId) {
        return (root, query, cb) ->
                visitId == null ? null : cb.equal(root.get("visitId"), visitId);
    }

    public static Specification<ChargeLine> hasStatuses(List<String> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty())
                        ? null
                        : root.get("status").in(statuses);
    }

    public static Specification<ChargeLine> hasSourceService(String sourceService) {
        return (root, query, cb) ->
                (sourceService == null || sourceService.isBlank())
                        ? null
                        : cb.equal(root.get("sourceService"), sourceService);
    }

    public static Specification<ChargeLine> createdAtBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}

