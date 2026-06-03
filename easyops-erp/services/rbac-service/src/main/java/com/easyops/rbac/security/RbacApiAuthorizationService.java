package com.easyops.rbac.security;

import com.easyops.rbac.dto.AuthorizationRequest;
import com.easyops.rbac.service.AuthorizationService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * In-process RBAC checks for rbac-service's own REST API (Phase 2 Wave 1). Avoids HTTP self-calls.
 */
@Component
@RequiredArgsConstructor
public class RbacApiAuthorizationService {

    private final AuthorizationService authorizationService;
    private final MeterRegistry meterRegistry;

    @Value("${rbac.api.enforcement.enabled:true}")
    private boolean enforcementEnabled;

    public void requireAny(UUID userId, UUID organizationId, String[][] resourceActionPairs) {
        if (!enforcementEnabled) {
            return;
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        for (String[] pair : resourceActionPairs) {
            AuthorizationRequest req = new AuthorizationRequest();
            req.setUserId(userId);
            req.setOrganizationId(organizationId);
            req.setResource(pair[0]);
            req.setAction(pair[1]);
            if (authorizationService.hasPermission(req)) {
                return;
            }
        }
        meterRegistry.counter("rbac_service.rbac.denied", "operation", "any").increment();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission for this operation");
    }

    public void requireSelfOrAny(UUID requesterId, UUID subjectUserId, UUID organizationId, String[][] pairs) {
        if (!enforcementEnabled) {
            return;
        }
        if (requesterId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id");
        }
        if (requesterId.equals(subjectUserId)) {
            return;
        }
        requireAny(requesterId, organizationId, pairs);
    }

    public void requireSelfOrUserManage(UUID requesterId, UUID subjectUserId, UUID organizationId) {
        if (!enforcementEnabled) {
            return;
        }
        requireSelfOrAny(requesterId, subjectUserId, organizationId, new String[][]{
                {"users", "manage"},
                {"system", "view"},
                {"system", "configure"},
        });
    }
}
