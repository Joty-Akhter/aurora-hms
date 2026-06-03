package com.easyops.hospitalbilling.domain.invoice;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    public static Specification<Invoice> hasPatientId(UUID patientId) {
        return (root, query, cb) ->
                patientId == null ? null : cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<Invoice> hasVisitId(UUID visitId) {
        return (root, query, cb) ->
                visitId == null ? null : cb.equal(root.get("visitId"), visitId);
    }

    public static Specification<Invoice> hasStatuses(List<String> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty())
                        ? null
                        : root.get("status").in(statuses);
    }

    public static Specification<Invoice> hasPayerType(String payerType) {
        return (root, query, cb) ->
                (payerType == null || payerType.isBlank())
                        ? null
                        : cb.equal(root.get("payerType"), payerType);
    }

    public static Specification<Invoice> issuedAtBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("issuedAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("issuedAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("issuedAt"), to);
        };
    }

    public static Specification<Invoice> createdAtBetween(OffsetDateTime from, OffsetDateTime to) {
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

