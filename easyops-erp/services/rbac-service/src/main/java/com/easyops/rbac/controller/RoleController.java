package com.easyops.rbac.controller;

import com.easyops.rbac.dto.RoleRequest;
import com.easyops.rbac.dto.RoleResponse;
import com.easyops.rbac.security.RbacApiAuthorizationService;
import com.easyops.rbac.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Role Controller
 * 
 * REST controller for role management operations.
 */
@RestController
@RequestMapping("/api/rbac/roles")
@RequiredArgsConstructor
public class RoleController {

    private static final String[][] ROLE_READ = {
            {"roles", "view"},
            {"roles", "manage"},
            {"system", "view"},
    };

    private static final String[][] ROLE_WRITE = {
            {"roles", "manage"},
            {"system", "configure"},
    };

    private final RoleService roleService;
    private final RbacApiAuthorizationService rbacApi;

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody RoleRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_WRITE);
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<RoleResponse>> getAllRoles(
            Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        Page<RoleResponse> response = roleService.getAllRoles(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RoleResponse>> getActiveRoles(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        List<RoleResponse> response = roleService.getActiveRoles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system")
    public ResponseEntity<List<RoleResponse>> getSystemRoles(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        List<RoleResponse> response = roleService.getSystemRoles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoleResponse>> searchRoles(
            @RequestParam("query") String query,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        List<RoleResponse> response = roleService.searchRoles(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RoleResponse> getRoleByCode(
            @PathVariable("code") String code,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        RoleResponse response = roleService.getRoleByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_READ);
        RoleResponse response = roleService.getRoleById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RoleRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_WRITE);
        RoleResponse response = roleService.updateRole(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_WRITE);
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<RoleResponse> addPermissionToRole(
            @PathVariable("roleId") UUID roleId,
            @PathVariable("permissionId") UUID permissionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_WRITE);
        RoleResponse response = roleService.addPermissionToRole(roleId, permissionId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<RoleResponse> removePermissionFromRole(
            @PathVariable("roleId") UUID roleId,
            @PathVariable("permissionId") UUID permissionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, ROLE_WRITE);
        RoleResponse response = roleService.removePermissionFromRole(roleId, permissionId, userId);
        return ResponseEntity.ok(response);
    }
}
