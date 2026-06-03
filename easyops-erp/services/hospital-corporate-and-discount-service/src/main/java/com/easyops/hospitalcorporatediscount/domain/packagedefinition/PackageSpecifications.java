package com.easyops.hospitalcorporatediscount.domain.packagedefinition;

import org.springframework.data.jpa.domain.Specification;

public final class PackageSpecifications {

    private PackageSpecifications() {}

    public static Specification<PackageDefinition> hasCode(String code) {
        if (code == null || code.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(cb.lower(root.get("code")), code.toLowerCase().trim());
    }

    public static Specification<PackageDefinition> isPublic(Boolean isPublic) {
        if (isPublic == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("isPublic"), isPublic);
    }
}
