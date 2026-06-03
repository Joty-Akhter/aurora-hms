package com.easyops.inventory.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class RbacRequestHeaders {

    private RbacRequestHeaders() {
    }

    public static UUID requireUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        try {
            return UUID.fromString(xUserId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id");
        }
    }
}
