package com.easyops.purchase.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

public final class OrganizationIdParser {

    private OrganizationIdParser() {
    }

    /** Required for RBAC on stub endpoints that pass org in JSON. */
    public static UUID fromMap(Map<String, ?> body) {
        if (body == null || !body.containsKey("organizationId")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationId is required");
        }
        Object v = body.get("organizationId");
        if (v instanceof UUID u) {
            return u;
        }
        return UUID.fromString(String.valueOf(v));
    }
}
