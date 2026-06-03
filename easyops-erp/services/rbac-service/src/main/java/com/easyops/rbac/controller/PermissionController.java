package com.easyops.rbac.controller;

import com.easyops.rbac.dto.PermissionRequest;
import com.easyops.rbac.dto.PermissionResponse;
import com.easyops.rbac.security.RbacApiAuthorizationService;
import com.easyops.rbac.service.PermissionService;
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
 * Permission Controller
 * 
 * REST controller for permission management operations.
 */
@RestController
@RequestMapping("/api/rbac/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private static final String[][] PERM_READ = {
            {"permissions", "view"},
            {"permissions", "manage"},
            {"system", "view"},
    };

    private static final String[][] PERM_WRITE = {
            {"permissions", "manage"},
            {"system", "configure"},
    };

    private final PermissionService permissionService;
    private final RbacApiAuthorizationService rbacApi;

    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody PermissionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_WRITE);
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PermissionResponse>> getAllPermissions(
            Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        Page<PermissionResponse> response = permissionService.getAllPermissions(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<PermissionResponse>> getActivePermissions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        List<PermissionResponse> response = permissionService.getActivePermissions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resource/{resource}")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByResource(
            @PathVariable String resource,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        List<PermissionResponse> response = permissionService.getPermissionsByResource(resource);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PermissionResponse>> searchPermissions(
            @RequestParam String query,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        List<PermissionResponse> response = permissionService.searchPermissions(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PermissionResponse> getPermissionByCode(
            @PathVariable String code,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        PermissionResponse response = permissionService.getPermissionByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getPermissionById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_READ);
        PermissionResponse response = permissionService.getPermissionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody PermissionRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_WRITE);
        PermissionResponse response = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        rbacApi.requireAny(userId, organizationId, PERM_WRITE);
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
