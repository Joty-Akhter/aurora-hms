package com.easyops.hospitalcard.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalServiceAuth {

    private final String expectedServiceKey;

    public InternalServiceAuth(
            @Value("${hospital-card.internal.service-key:}") String expectedServiceKey) {
        this.expectedServiceKey = expectedServiceKey != null ? expectedServiceKey.trim() : "";
    }

    public void assertServiceKey(String serviceKey) {
        if (!StringUtils.hasText(expectedServiceKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Internal card API is disabled (hospital-card.internal.service-key not configured)");
        }
        String presented = serviceKey != null ? serviceKey.trim() : "";
        if (!expectedServiceKey.equals(presented)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service key");
        }
    }

    public boolean isEnabled() {
        return StringUtils.hasText(expectedServiceKey);
    }
}
