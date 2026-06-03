package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class SlotTemplateSpecifications {

    private SlotTemplateSpecifications() {}

    public static Specification<SlotTemplate> hasResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<SlotTemplate> hasBranchId(UUID branchId) {
        if (branchId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<SlotTemplate> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }
}
