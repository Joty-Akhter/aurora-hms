package com.easyops.rbac.controller;

import com.easyops.rbac.dto.*;
import com.easyops.rbac.security.RbacApiAuthorizationService;
import com.easyops.rbac.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authorization Controller
 * 
 * REST controller for authorization and user role management.
 */
@RestController
@RequestMapping("/api/rbac/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

    private static final String[][] ASSIGN_ROLES = {
            {"roles", "manage"},
            {"system", "configure"},
    };

    private static final String[][] VIEW_OTHER_USER_ROLES = {
            {"roles", "view"},
            {"roles", "manage"},
            {"users", "manage"},
            {"system", "view"},
    };

    private static final String[][] VIEW_OTHER_USER_PERMISSIONS = {
            {"roles", "view"},
            {"roles", "manage"},
            {"users", "manage"},
            {"system", "view"},
    };

    private static final String[][] ROLE_READ = {
            {"roles", "view"},
            {"roles", "manage"},
            {"system", "view"},
    };

    private final AuthorizationService authorizationService;
    private final RbacApiAuthorizationService rbacApi;

    @PostMapping("/users/roles")
    public ResponseEntity<List<RoleResponse>> assignRolesToUser(
            @Valid @RequestBody UserRoleRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ASSIGN_ROLES);
        List<RoleResponse> response = authorizationService.assignRolesToUser(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(requesterId, organizationId, ASSIGN_ROLES);
        authorizationService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/roles")
    public ResponseEntity<Void> removeAllRolesFromUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(requesterId, organizationId, ASSIGN_ROLES);
        authorizationService.removeAllRolesFromUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID organizationId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        rbacApi.requireSelfOrAny(requesterId, userId, organizationId, VIEW_OTHER_USER_ROLES);
        List<RoleResponse> response = authorizationService.getUserRoles(userId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<List<PermissionResponse>> getUserPermissions(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID organizationId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        rbacApi.requireSelfOrAny(requesterId, userId, organizationId, VIEW_OTHER_USER_PERMISSIONS);
        List<PermissionResponse> response = authorizationService.getUserPermissions(userId, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/organization-context")
    public ResponseEntity<UserOrganizationContextResponse> getUserOrganizationContext(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireSelfOrUserManage(requesterId, userId, organizationId);
        return ResponseEntity.ok(authorizationService.getUserOrganizationContext(userId));
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkPermission(
            @Valid @RequestBody AuthorizationRequest request,
            @RequestHeader("X-User-Id") UUID requesterId) {
        if (!requesterId.equals(request.getUserId())) {
            rbacApi.requireAny(requesterId, request.getOrganizationId(), ASSIGN_ROLES);
        }
        boolean hasPermission = authorizationService.hasPermission(request);
        Map<String, Boolean> response = new HashMap<>();
        response.put("authorized", hasPermission);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/has-role/{roleCode}")
    public ResponseEntity<Map<String, Boolean>> checkRole(
            @PathVariable UUID userId,
            @PathVariable String roleCode,
            @RequestParam(required = false) UUID organizationId,
            @RequestHeader("X-User-Id") UUID requesterId) {
        rbacApi.requireSelfOrAny(requesterId, userId, organizationId, VIEW_OTHER_USER_ROLES);
        boolean hasRole = authorizationService.hasRole(userId, roleCode, organizationId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasRole", hasRole);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles/{roleId}/users")
    public ResponseEntity<List<UUID>> getUsersByRole(
            @PathVariable UUID roleId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        List<UUID> userIds = authorizationService.getUsersByRole(roleId);
        return ResponseEntity.ok(userIds);
    }
}
